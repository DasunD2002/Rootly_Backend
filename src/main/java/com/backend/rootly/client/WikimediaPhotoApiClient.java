package com.backend.rootly.client;

import com.backend.rootly.client.WikimediaPhotoClient.Photo;
import com.backend.rootly.config.ExploreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.backend.rootly.client.WikimediaPhotoMetadata.*;

/** Batches photo metadata requests and respects provider rate limits. */
@Component
@RequiredArgsConstructor
public class WikimediaPhotoApiClient {
    private static final String QUERY = "query";
    private static final int TOO_MANY_REQUESTS = 429;
    private static final int MAX_CACHE = 2_000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private final HttpClient http;
    private final JsonMapper mapper;
    private final ExploreProperties properties;
    private final Clock clock;
    private final Map<String, Photo> fileCache = new ConcurrentHashMap<>();
    private final Map<URI, Instant> nextAttempts = new ConcurrentHashMap<>();

    Map<String, Photo> resolveFiles(List<String> titles) {
        Map<String, Photo> result = new LinkedHashMap<>();
        for (String title : titles) {
            Photo cached = fileCache.get(title);
            if (cached != null) result.put(title, cached);
        }
        List<String> pending = titles.stream().filter(title -> !result.containsKey(title)).toList();
        for (int start = 0; start < pending.size(); start += 50) {
            List<String> batch = pending.subList(start, Math.min(start + 50, pending.size()));
            JsonNode data = request(properties.getCommonsUrl(),
                    "action=query&format=json&formatversion=2&prop=imageinfo&iiprop=url%7Cmime"
                            + "&iiurlwidth=960&redirects=1&titles=" + encode(String.join("|", batch)));
            Map<String, Photo> resolved = readFiles(data);
            result.putAll(resolved);
            copyAliases(data.path(QUERY).path("redirects"), result);
            copyAliases(data.path(QUERY).path("normalized"), result);
        }
        fileCache.putAll(result);
        if (fileCache.size() > MAX_CACHE) fileCache.clear();
        return result;
    }

    private Map<String, Photo> readFiles(JsonNode data) {
        Map<String, Photo> result = new LinkedHashMap<>();
        for (JsonNode page : data.path(QUERY).path("pages")) {
            Photo photo = readPhoto(page);
            String title = text(page.path("title"));
            if (photo != null && title != null) result.put(title, photo);
        }
        return result;
    }

    JsonNode articles(List<String> titles) {
        return request(properties.getWikipediaUrl(),
                "action=query&format=json&formatversion=2&prop=pageimages%7Cimages&piprop=thumbnail%7Cname"
                        + "&pithumbsize=960&pilicense=free&imlimit=20&redirects=1&titles=" + encode(String.join("|", titles)));
    }

    Photo depictedPhoto(String id) {
        JsonNode data = request(properties.getCommonsUrl(),
                "action=query&format=json&formatversion=2&generator=search&gsrnamespace=6&gsrlimit=1"
                        + "&prop=imageinfo&iiprop=url%7Cmime&iiurlwidth=960&gsrsearch=" + encode("haswbstatement:P180=" + id));
        for (JsonNode page : data.path(QUERY).path("pages")) {
            Photo photo = readPhoto(page);
            if (photo != null) return photo;
        }
        return null;
    }

    private JsonNode request(URI endpoint, String query) {
        Instant nextAttempt = nextAttempts.get(endpoint);
        if (nextAttempt != null && clock.instant().isBefore(nextAttempt)) return mapper.createObjectNode();
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint + "?" + query)).timeout(REQUEST_TIMEOUT)
                .header("User-Agent", properties.getUserAgent()).header("Accept", "application/json").GET().build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == java.net.HttpURLConnection.HTTP_OK) return mapper.readTree(response.body());
            if (response.statusCode() == TOO_MANY_REQUESTS || response.statusCode() == java.net.HttpURLConnection.HTTP_UNAVAILABLE) {
                nextAttempts.put(endpoint, clock.instant().plusSeconds(retryDelay(response)));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            return mapper.createObjectNode(); // Keep place records available during an upstream failure.
        }
        return mapper.createObjectNode();
    }

    private static long retryDelay(HttpResponse<?> response) {
        try {
            return Math.max(60, Math.min(3600, Long.parseLong(response.headers().firstValue("Retry-After").orElse("60"))));
        } catch (NumberFormatException exception) {
            return 60;
        }
    }

}
