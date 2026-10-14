package com.backend.rootly.client;

import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.mapper.WikidataPlaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.zip.GZIPInputStream;

@Component
@RequiredArgsConstructor
public class WikidataPlacesClient {

    private final HttpClient http;
    private final JsonMapper mapper;
    private final WikidataPlaceMapper placeMapper;
    private final Clock clock;
    private final URI queryUri;
    private final ExploreProperties properties;

    public ExploreCatalog fetch() {
        HttpRequest request = HttpRequest.newBuilder(queryUri).timeout(properties.getRequestTimeout())
                .header("User-Agent", properties.getUserAgent()).header("Accept", "application/sparql-results+json")
                .header("Accept-Encoding", "gzip").GET().build();
        try {
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new PlacesUnavailableException("Wikidata returned HTTP " + response.statusCode(),
                        retryAfter(response.headers().firstValue("Retry-After").orElse("60")));
            }
            try (InputStream input = "gzip".equalsIgnoreCase(response.headers().firstValue("Content-Encoding").orElse(""))
                    ? new GZIPInputStream(new ByteArrayInputStream(response.body()))
                    : new ByteArrayInputStream(response.body())) {
                JsonNode root = mapper.readTree(input);
                return placeMapper.map(root, clock.instant());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PlacesUnavailableException("Place lookup was interrupted", exception);
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            throw new PlacesUnavailableException("Could not read Wikidata places", exception);
        }
    }

    private Duration retryAfter(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return Duration.ofSeconds(Math.max(60, Math.min(seconds, 86_400)));
        } catch (NumberFormatException exception) {
            try {
                Duration duration = Duration.between(clock.instant(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                return duration.compareTo(Duration.ofSeconds(60)) > 0 ? duration : Duration.ofSeconds(60);
            } catch (DateTimeParseException ignored) {
                return Duration.ofSeconds(60);
            }
        }
    }
}
