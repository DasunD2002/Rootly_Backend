package com.backend.rootly.client;

import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ProvinceCatalog;
import com.backend.rootly.enums.ExploreProvince;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.mapper.WikidataProvinceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
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
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Component
@RequiredArgsConstructor
public class WikidataProvinceClient {
    private final HttpClient http;
    private final JsonMapper mapper;
    private final WikidataProvinceMapper provinceMapper;
    private final Clock clock;
    private final Map<ExploreProvince, URI> provinceQueryUris;
    private final ExploreProperties properties;

    public ProvinceCatalog fetch(ExploreProvince province) {
        HttpRequest request = HttpRequest.newBuilder(provinceQueryUris.get(province))
                .timeout(properties.getRequestTimeout())
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/sparql-results+json")
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
                return provinceMapper.map(mapper.readTree(input), province, clock.instant());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PlacesUnavailableException("Province lookup was interrupted", exception);
        } catch (IOException | tools.jackson.core.JacksonException exception) {
            throw new PlacesUnavailableException("Could not read Wikidata province data", exception);
        }
    }

    private Duration retryAfter(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return Duration.ofSeconds(Math.max(60, Math.min(seconds, 86_400)));
        } catch (NumberFormatException exception) {
            try {
                long seconds = Duration.between(clock.instant(),
                        ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toSeconds();
                return Duration.ofSeconds(Math.max(60, Math.min(seconds, 86_400)));
            } catch (DateTimeParseException ignored) {
                return Duration.ofSeconds(60);
            }
        }
    }
}
