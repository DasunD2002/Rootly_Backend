package com.backend.rootly.client;

import com.backend.rootly.config.ExploreConfiguration;
import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.mapper.WikidataPlaceMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WikidataPlacesClientTests {
    private HttpServer server;
    private WikidataPlacesClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        ExploreProperties properties = new ExploreProperties();
        properties.setWikidataUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/sparql"));
        properties.setRequestTimeout(Duration.ofSeconds(2));
        properties.setUserAgent("RootlyBackend/Test");
        URI queryUri = new ExploreConfiguration(properties)
                .wikidataQueryUri(new ClassPathResource("queries/explore-places.sparql"));
        client = new WikidataPlacesClient(HttpClient.newHttpClient(), JsonMapper.builder().build(),
                new WikidataPlaceMapper(), Clock.systemUTC(), queryUri, properties);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsIdentifiedEncodedQueryAndReadsGzipResults() throws IOException {
        byte[] fixture = new ClassPathResource("explore-places-fixture.json").getContentAsByteArray();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(fixture);
        }
        byte[] response = bytes.toByteArray();
        server.createContext("/sparql", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("User-Agent")).isEqualTo("RootlyBackend/Test");
            assertThat(exchange.getRequestURI().getQuery()).contains("format=json", "Q854");
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        assertThat(client.fetch().getPlaces()).hasSize(2);
    }

    @Test
    void preservesProviderBackoffAndRejectsMalformedJson() {
        server.createContext("/sparql", exchange -> {
            exchange.getResponseHeaders().set("Retry-After", "180");
            exchange.sendResponseHeaders(429, -1);
            exchange.close();
        });
        assertThatThrownBy(client::fetch).isInstanceOfSatisfying(PlacesUnavailableException.class,
                exception -> assertThat(exception.getRetryAfter()).isEqualTo(Duration.ofSeconds(180)));
        server.removeContext("/sparql");
        server.createContext("/sparql", exchange -> {
            byte[] response = "not-json".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        assertThatThrownBy(client::fetch).isInstanceOf(PlacesUnavailableException.class);
    }
}
