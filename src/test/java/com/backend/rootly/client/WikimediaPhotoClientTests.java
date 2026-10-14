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
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class WikimediaPhotoClientTests {
    private static final String THUMB = "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/ab/Temple.jpg/960px-Temple.jpg";
    private HttpServer server;
    private WikimediaPhotoClient client;

    @BeforeEach
    void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        ExploreProperties properties = new ExploreProperties();
        properties.setCommonsUrl(URI.create(base + "/commons"));
        properties.setWikipediaUrl(URI.create(base + "/wiki"));
        Clock clock = Clock.systemUTC();
        client = new WikimediaPhotoClient(new WikimediaPhotoApiClient(HttpClient.newHttpClient(),
                JsonMapper.builder().build(), properties, clock), clock, new CuratedExplorePhotos(JsonMapper.builder().build()));
    }

    @AfterEach
    void cleanup() {
        client.close();
        server.stop(0);
    }

    private static void json(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String fileResponse(String title) {
        return """
                {"query":{"pages":[{"title":"%s","imageinfo":[{"thumburl":"%s","mime":"image/jpeg",
                "descriptionurl":"https://commons.wikimedia.org/wiki/File:Temple_A+B.jpg"}]}]}}
                """.formatted(title, THUMB);
    }

    private static ExplorePlaceDTO place(String id, String name, String image, String article) {
        return new ExplorePlaceDTO(id, name, "Sri Lanka", "Sacred Sites", "sacred-sites",
                new ExploreLocationDTO(7.9, 81.0), "Existing description", image, null,
                "https://www.wikidata.org/wiki/" + id, article);
    }

    @Test
    void resolvesCommonsFilesInOneBatchPreservesPlusAndCachesPhotos() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/commons", exchange -> {
            requests.incrementAndGet();
            String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            assertThat(query).contains("iiurlwidth=960", "File:temple A+B.jpg|File:Temple Two.jpg");
            json(exchange, 200, """
                    {"query":{"normalized":[{"from":"File:temple A+B.jpg","to":"File:Temple A+B.jpg"}],
                      "redirects":[{"from":"File:Temple A+B.jpg","to":"File:Actual Temple.jpg"}],"pages":[
                      {"title":"File:Actual Temple.jpg","imageinfo":[{"thumburl":"%s","mime":"image/jpeg",
                       "descriptionurl":"https://commons.wikimedia.org/wiki/File:Actual_Temple.jpg"}]},
                      {"title":"File:Temple Two.jpg","imageinfo":[{"thumburl":"%s","mime":"image/jpeg"}]}
                    ]}}
                    """.formatted(THUMB, THUMB));
        });
        List<ExplorePlaceDTO> places = List.of(
                place("Q100", "Temple One", "https://commons.wikimedia.org/wiki/Special:FilePath/temple_A%2BB.jpg", null),
                place("Q200", "Temple Two", "https://commons.wikimedia.org/wiki/Special:FilePath/Temple_Two.jpg", null));
        var first = client.enrichPlaces(places);
        assertThat(first).extracting(ExplorePlaceDTO::getImageUrl).containsExactly(THUMB, THUMB);
        assertThat(first.get(0).getImageSourceUrl()).contains("File:Actual_Temple.jpg");
        assertThat(first.get(0).getDescription()).isEqualTo("Existing description");
        assertThat(client.enrichPlaces(places)).isEqualTo(first);
        assertThat(requests).hasValue(1);
    }

    @Test
    void recoversAMissingPhotoFromTheSameWikipediaArticleIncludingRedirects() {
        server.createContext("/wiki", exchange -> {
            String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            assertThat(query).contains("pithumbsize=960", "pilicense=free", "titles=Old Title");
            json(exchange, 200, """
                    {"query":{"redirects":[{"from":"Old Title","to":"Actual Temple"}],
                      "pages":[{"title":"Actual Temple","pageimage":"Temple.jpg","thumbnail":{"source":"%s"}}]}}
                    """.formatted(THUMB));
        });
        var result = client.enrichPlaces(List.of(place("Q100", "Actual Temple", null, "https://en.wikipedia.org/wiki/Old_Title")));
        assertThat(result.get(0).getImageUrl()).isEqualTo(THUMB);
        assertThat(result.get(0).getImageSourceUrl()).isEqualTo("https://en.wikipedia.org/wiki/File:Temple.jpg");
    }

    @Test
    void ignoresUnrelatedTemplateImagesAndSearchesTheExactDepictedEntity() {
        server.createContext("/wiki", exchange -> json(exchange, 200, """
                {"query":{"pages":[{"title":"Ariyalai Siddhivinayakar Temple",
                  "images":[{"title":"File:Jetavanaramaya Stupa.jpg"},{"title":"File:Question book.svg"}]}]}}
                """));
        server.createContext("/commons", exchange -> {
            String query = URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            assertThat(query).contains("haswbstatement:P180=Q4791177");
            json(exchange, 200, fileResponse("File:Ariyalai.jpg"));
        });
        var result = client.enrichPlaces(List.of(place("Q4791177", "Ariyalai Siddhivinayakar Temple",
                null, "https://en.wikipedia.org/wiki/Ariyalai_Siddhivinayakar_Temple")));
        assertThat(result.get(0).getImageUrl()).isEqualTo(THUMB);
    }

    @Test
    void usesAMatchingArticlePhotoWhenNoLeadPhotoWasSelected() {
        server.createContext("/wiki", exchange -> json(exchange, 200, """
                {"query":{"pages":[{"title":"Balana Fort","images":[{"title":"File:Front view, Balana fort.jpg"}]}]}}
                """));
        server.createContext("/commons", exchange -> {
            assertThat(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8))
                    .contains("titles=File:Front view, Balana fort.jpg");
            json(exchange, 200, fileResponse("File:Front view, Balana fort.jpg"));
        });
        var result = client.enrichPlaces(List.of(place("Q28937771", "Balana Fort", null, "https://en.wikipedia.org/wiki/Balana_Fort")));
        assertThat(result.get(0).getImageUrl()).isEqualTo(THUMB);
    }

    @Test
    void usesAReviewedFileForTheExactPlaceWithoutMatchingAnotherTemple() {
        server.createContext("/commons", exchange -> {
            assertThat(URLDecoder.decode(exchange.getRequestURI().getRawQuery(), StandardCharsets.UTF_8))
                    .contains("titles=File:Athkanda Raja Maha Viharaya.jpg");
            json(exchange, 200, fileResponse("File:Athkanda Raja Maha Viharaya.jpg"));
        });
        var result = client.enrichPlaces(List.of(place("Q19700758", "Athkanda Raja Maha Viharaya", null, null)));
        assertThat(result.get(0).getImageUrl()).isEqualTo(THUMB);
    }

    @Test
    void upstreamFailuresKeepPlaceRecordsAndBackOffOtherPhotoLookups() {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/commons", exchange -> {
            requests.incrementAndGet();
            exchange.getResponseHeaders().set("Retry-After", "60");
            json(exchange, 429, "{}");
        });
        server.createContext("/wiki", exchange -> json(exchange, 503, "{}"));
        var original = place("Q100", "Real Site", null, "https://en.wikipedia.org/wiki/Real_Site");
        var result = client.enrichPlaces(List.of(original));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Real Site");
        assertThat(result.get(0).getImageUrl()).isNull();
        client.enrichPlaces(List.of(original));
        client.enrichPlaces(List.of(place("Q200", "Second Site", null, null)));
        assertThat(requests).hasValue(1);
    }

    @Test
    void rejectsHtmlImagesAndUntrustedOrMalformedHosts() {
        assertThat(WikimediaPhotoClient.trustedImage(THUMB)).isTrue();
        for (String url : List.of("https:opaque", "https://thumb.wikimedia.org.evil.example/wikipedia/a.jpg",
                "http://thumb.wikimedia.org/wikipedia/a.jpg", "https://upload.wikimedia.org@evil.example/wikipedia/a.jpg")) {
            assertThat(WikimediaPhotoClient.trustedImage(url)).isFalse();
        }
        server.createContext("/commons", exchange -> json(exchange, 200, """
                {"query":{"pages":[{"title":"File:Broken.jpg","imageinfo":[{"thumburl":"%s","mime":"text/html"}]}]}}
                """.formatted(THUMB)));
        var result = client.enrichPlaces(List.of(place("Q100", "Real Site",
                "https://commons.wikimedia.org/wiki/Special:FilePath/Broken.jpg", null)));
        assertThat(result.get(0).getImageUrl()).isNull();
    }
}
