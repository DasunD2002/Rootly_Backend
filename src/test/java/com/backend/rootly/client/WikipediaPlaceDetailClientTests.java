package com.backend.rootly.client;

import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.dto.response.ExploreLocationDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WikipediaPlaceDetailClientTests {
    private HttpServer server;
    private WikipediaPlaceDetailClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        ExploreProperties properties = new ExploreProperties();
        properties.setWikipediaUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api.php"));
        properties.setUserAgent("RootlyBackend/Test");
        client = new WikipediaPlaceDetailClient(HttpClient.newHttpClient(),
                JsonMapper.builder().build(), properties, Clock.systemUTC());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static ExplorePlaceDTO place(String article) {
        return new ExplorePlaceDTO("Q100", "Gal Vihara", "Polonnaruwa, Sri Lanka",
                "Sacred Sites", "sacred-sites", new ExploreLocationDTO(7.9668, 81.0041),
                "Rock temple", null, null, "https://www.wikidata.org/wiki/Q100", article);
    }

    @Test
    void loadsNarrativeAndSpecificPhotoAndCachesTheResult() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/api.php", exchange -> {
            requests.incrementAndGet();
            assertThat(exchange.getRequestURI().getRawQuery()).contains("titles=Gal+Vihara", "exintro=1");
            byte[] body = ("""
                    {"query":{"pages":[{"title":"Gal Vihara",
                      "extract":"A detailed first paragraph.\\n\\nA second paragraph about the sculptures.",
                      "pageimage":"Gal Vihara.jpg",
                      "thumbnail":{"source":"https://upload.wikimedia.org/wikipedia/commons/thumb/example.jpg"}}]}}
                    """).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        ExplorePlaceDTO result = client.enrich(place("https://en.wikipedia.org/wiki/Gal_Vihara"));
        assertThat(result.getDescription()).contains("second paragraph");
        assertThat(result.getImageUrl()).startsWith("https://upload.wikimedia.org/");
        assertThat(result.getImageSourceUrl()).isEqualTo("https://en.wikipedia.org/wiki/File:Gal%20Vihara.jpg");
        assertThat(result.getSourceUrl()).isEqualTo("https://www.wikidata.org/wiki/Q100");
        assertThat(client.enrich(place("https://en.wikipedia.org/wiki/Gal_Vihara"))).isSameAs(result);
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void keepsCatalogDataWhenArticleIsUnavailable() {
        assertThat(client.enrich(place(null)).getDescription()).isEqualTo("Rock temple");
        assertThat(client.enrich(place("https://untrusted.invalid/wiki/Gal_Vihara")).getDescription())
                .isEqualTo("Rock temple");
        server.createContext("/api.php", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        assertThat(client.enrich(place("https://en.wikipedia.org/wiki/Gal_Vihara")).getDescription())
                .isEqualTo("Rock temple");
    }
}
