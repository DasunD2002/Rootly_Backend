package com.backend.rootly.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "rootly.explore")
public class ExploreProperties {

    private URI wikidataUrl = URI.create("https://query.wikidata.org/sparql");
    private URI wikipediaUrl = URI.create("https://en.wikipedia.org/w/api.php");
    private String userAgent = "RootlyBackend/0.1 (Sri Lanka heritage explorer)";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofSeconds(25);
    private Duration cacheTtl = Duration.ofMinutes(15);
    private Duration maxStale = Duration.ofHours(24);
    private List<String> allowedOriginPatterns = List.of("http://localhost:*", "http://127.0.0.1:*");

    @PostConstruct
    public void validate() {
        if (cacheTtl.isNegative() || cacheTtl.isZero() || maxStale.compareTo(cacheTtl) < 0) {
            throw new IllegalArgumentException("Cache TTL must be positive and max-stale must be at least the TTL");
        }
    }
}
