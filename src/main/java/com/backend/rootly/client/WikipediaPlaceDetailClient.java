package com.backend.rootly.client;

import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads article text and a place-specific lead photo only when a detail page is opened. */
@Component
@Log4j2
@RequiredArgsConstructor
public class WikipediaPlaceDetailClient {
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private final HttpClient http;
    private final JsonMapper mapper;
    private final ExploreProperties properties;
    private final Clock clock;
    private final Map<String, CachedPlace> cache = new ConcurrentHashMap<>();

    public ExplorePlaceDTO enrich(ExplorePlaceDTO place) {
        String title = articleTitle(place.getWikipediaUrl());
        if (title == null) {
            return place;
        }
        CachedPlace cached = cache.get(place.getId());
        if (cached != null && clock.instant().isBefore(cached.fetchedAt().plus(CACHE_TTL))) {
            return cached.place();
        }
        return fetch(place, title);
    }

    private ExplorePlaceDTO fetch(ExplorePlaceDTO place, String title) {
        URI requestUri = URI.create(properties.getWikipediaUrl() + "?action=query&format=json&formatversion=2"
                + "&prop=extracts%7Cpageimages&explaintext=1&exintro=1"
                + "&piprop=thumbnail%7Cname&pithumbsize=1200&titles="
                + URLEncoder.encode(title, StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/json")
                .GET().build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                if (log.isWarnEnabled()) {
                    log.warn("Wikipedia detail request for {} returned HTTP {}", place.getId(), response.statusCode());
                }
                return place;
            }
            return readDetails(place, response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (log.isWarnEnabled()) {
                log.warn("Wikipedia detail request for {} was interrupted", place.getId());
            }
        } catch (IOException | tools.jackson.core.JacksonException | IllegalArgumentException exception) {
            if (log.isWarnEnabled()) {
                log.warn("Could not load Wikipedia detail for {}: {}", place.getId(), exception.getMessage());
            }
        }
        return place;
    }

    private ExplorePlaceDTO readDetails(ExplorePlaceDTO place, byte[] body) {
        JsonNode pages = mapper.readTree(body).path("query").path("pages");
        if (!pages.isArray() || pages.isEmpty() || !pages.get(0).path("missing").isMissingNode()) {
            return place;
        }
        return mergeDetails(place, pages.get(0));
    }

    private ExplorePlaceDTO mergeDetails(ExplorePlaceDTO place, JsonNode page) {
        String extract = text(page.path("extract"));
        String thumbnail = text(page.path("thumbnail").path("source"));
        String imageName = text(page.path("pageimage"));
        String image = thumbnail != null && thumbnail.startsWith("https://upload.wikimedia.org/")
                ? thumbnail : null;
        String imageSource = image != null && imageName != null
                ? "https://en.wikipedia.org/wiki/File:"
                + URLEncoder.encode(imageName, StandardCharsets.UTF_8).replace("+", "%20") : null;
        ExplorePlaceDTO result = new ExplorePlaceDTO(place.getId(), place.getName(), place.getSubtitle(),
                place.getCategory(), place.getCategoryId(), place.getLocation(),
                extract == null ? place.getDescription() : extract,
                image == null ? place.getImageUrl() : image,
                image == null ? place.getImageSourceUrl() : imageSource,
                place.getSourceUrl(), place.getWikipediaUrl());
        cache.put(place.getId(), new CachedPlace(result, clock.instant()));
        return result;
    }

    private static String text(JsonNode node) {
        return node.isString() && !node.asString().isBlank() ? node.asString().trim() : null;
    }

    private static String articleTitle(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"en.wikipedia.org".equals(uri.getHost())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getRawPath() == null || !uri.getRawPath().startsWith("/wiki/")) {
                return null;
            }
            return java.net.URLDecoder.decode(uri.getRawPath().substring("/wiki/".length()).replace("+", "%2B"),
                    StandardCharsets.UTF_8).replace('_', ' ');
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record CachedPlace(ExplorePlaceDTO place, Instant fetchedAt) {
    }
}
