package com.backend.rootly.client;

import com.backend.rootly.dto.response.ExplorePlaceDTO;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.backend.rootly.client.WikimediaPhotoMetadata.*;

/** Resolves actual subject photos; never substitutes a generic category image. */
@Component
@RequiredArgsConstructor
public class WikimediaPhotoClient {
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration MISS_TTL = Duration.ofMinutes(5);
    private static final int MAX_CACHE = 2_000;
    private static final Pattern ENTITY = Pattern.compile("Q[1-9][0-9]*");
    private static final String QUERY = "query";
    private final WikimediaPhotoApiClient api;
    private final Clock clock;
    private final CuratedExplorePhotos curated;
    private final Map<Subject, CachedPhoto> cache = new ConcurrentHashMap<>();
    private final ExecutorService searches = Executors.newFixedThreadPool(4);

    public record Subject(String id, String name, String imageUrl, String imageSourceUrl, String wikipediaUrl) { }
    public record Photo(String url, String sourceUrl) { }
    private record CachedPhoto(Photo photo, Instant expiresAt) { }

    public List<ExplorePlaceDTO> enrichPlaces(List<ExplorePlaceDTO> places) {
        List<Subject> subjects = places.stream().map(place -> new Subject(place.getId(), place.getName(),
                place.getImageUrl(), place.getImageSourceUrl(), place.getWikipediaUrl())).toList();
        Map<String, Photo> photos = resolve(subjects);
        return places.stream().map(place -> {
            Photo photo = photos.get(place.getId());
            return new ExplorePlaceDTO(place.getId(), place.getName(), place.getSubtitle(), place.getCategory(),
                    place.getCategoryId(), place.getLocation(), place.getDescription(),
                    photo == null ? null : photo.url(), photo == null ? null : photo.sourceUrl(),
                    place.getSourceUrl(), place.getWikipediaUrl());
        }).toList();
    }

    public synchronized Map<String, Photo> resolve(List<Subject> subjects) {
        Map<String, Photo> result = new LinkedHashMap<>();
        List<Subject> pending = new ArrayList<>();
        collectCached(subjects, result, pending);
        for (int start = 0; start < pending.size(); start += 50) {
            resolveBatch(pending.subList(start, Math.min(start + 50, pending.size())), result);
        }
        for (Subject subject : pending) {
            Photo photo = result.get(subject.id());
            cache.put(subject, new CachedPhoto(photo, clock.instant().plus(photo == null ? MISS_TTL : TTL)));
        }
        if (cache.size() > MAX_CACHE) cache.clear();
        return result;
    }

    private void collectCached(List<Subject> subjects, Map<String, Photo> result, List<Subject> pending) {
        for (Subject subject : subjects) {
            CachedPhoto existing = cache.get(subject);
            if (existing != null && clock.instant().isBefore(existing.expiresAt())) {
                if (existing.photo() != null) result.put(subject.id(), existing.photo());
            } else {
                pending.add(subject);
            }
        }
    }

    public static boolean trustedImage(String url) {
        return WikimediaPhotoMetadata.trustedImage(url);
    }

    private void resolveBatch(List<Subject> subjects, Map<String, Photo> result) {
        List<String> files = subjects.stream().map(curated::fileFor)
                .filter(title -> title != null).distinct().toList();
        Map<String, Photo> commons = api.resolveFiles(files);
        for (Subject subject : subjects) {
            Photo photo = commons.get(curated.fileFor(subject));
            if (photo == null && trustedImage(subject.imageUrl())) {
                photo = new Photo(subject.imageUrl(), subject.imageSourceUrl());
            }
            if (photo != null) result.put(subject.id(), photo);
        }
        List<Subject> missing = subjects.stream().filter(subject -> !result.containsKey(subject.id())).toList();
        resolveArticles(missing, result);
        resolveDepictedSubjects(missing.stream().filter(subject -> !result.containsKey(subject.id())).toList(), result);
    }

    private void resolveArticles(List<Subject> subjects, Map<String, Photo> result) {
        Map<String, Subject> byTitle = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            String title = articleTitle(subject.wikipediaUrl());
            if (title != null) byTitle.put(title, subject);
        }
        if (byTitle.isEmpty()) return;
        JsonNode data = api.articles(new ArrayList<>(byTitle.keySet()));
        copySubjectAliases(data.path(QUERY).path("normalized"), byTitle);
        copySubjectAliases(data.path(QUERY).path("redirects"), byTitle);
        Map<String, String> candidates = new LinkedHashMap<>();
        for (JsonNode page : data.path(QUERY).path("pages")) {
            Subject subject = byTitle.get(text(page.path("title")));
            if (subject != null) readArticlePhoto(page, subject, candidates, result);
        }
        Map<String, Photo> photos = api.resolveFiles(new ArrayList<>(candidates.values()));
        candidates.forEach((id, title) -> {
            Photo photo = photos.get(title);
            if (photo != null) result.put(id, photo);
        });
    }

    private static void copySubjectAliases(JsonNode aliases, Map<String, Subject> subjects) {
        for (JsonNode alias : aliases) {
            Subject subject = subjects.get(text(alias.path("from")));
            String to = text(alias.path("to"));
            if (subject != null && to != null) subjects.put(to, subject);
        }
    }

    private static void readArticlePhoto(JsonNode page, Subject subject,
                                         Map<String, String> candidates, Map<String, Photo> result) {
        String thumbnail = text(page.path("thumbnail").path("source"));
        String name = text(page.path("pageimage"));
        if (trustedImage(thumbnail) && name != null) {
            result.put(subject.id(), new Photo(thumbnail, "https://en.wikipedia.org/wiki/File:" + encode(name).replace("+", "%20")));
            return;
        }
        for (JsonNode image : page.path("images")) {
            String title = text(image.path("title"));
            if (matchesSubjectFile(subject.name(), title)) {
                candidates.put(subject.id(), title);
                return;
            }
        }
    }

    private void resolveDepictedSubjects(List<Subject> subjects, Map<String, Photo> result) {
        Map<Subject, Future<Photo>> pending = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            if (subject.id() != null && ENTITY.matcher(subject.id()).matches()) {
                pending.put(subject, searches.submit(() -> api.depictedPhoto(subject.id())));
            }
        }
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        for (Map.Entry<Subject, Future<Photo>> entry : pending.entrySet()) {
            try {
                Photo photo = entry.getValue().get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (photo != null) result.put(entry.getKey().id(), photo);
            } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
                entry.getValue().cancel(true);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        pending.values().forEach(future -> future.cancel(true));
    }

    @PreDestroy
    public void close() {
        searches.shutdownNow();
    }
}
