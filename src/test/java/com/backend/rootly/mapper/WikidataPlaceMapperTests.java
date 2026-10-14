package com.backend.rootly.mapper;

import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.exception.PlacesUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WikidataPlaceMapperTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private final WikidataPlaceMapper mapper = new WikidataPlaceMapper();

    @Test
    void mapsCoordinatesDeduplicatesAndPreservesRealSourceFields() throws IOException {
        String fixture = new ClassPathResource("explore-places-fixture.json")
                .getContentAsString(StandardCharsets.UTF_8);
        ExploreCatalog catalog = mapper.map(json.readTree(fixture), Instant.EPOCH);
        assertThat(catalog.getPlaces()).extracting(ExplorePlaceDTO::getId).containsExactly("Q200", "Q100");
        ExplorePlaceDTO temple = catalog.getPlaces().get(1);
        assertThat(temple.getLocation().getLatitude()).isEqualTo(7.9668);
        assertThat(temple.getLocation().getLongitude()).isEqualTo(81.0041);
        assertThat(temple.getCategoryId()).isEqualTo("sacred-sites");
        assertThat(temple.getSubtitle()).isEqualTo("Test District, Sri Lanka");
        assertThat(temple.getImageUrl()).startsWith("https://commons.wikimedia.org/");
        assertThat(temple.getImageSourceUrl()).isEqualTo("https://commons.wikimedia.org/wiki/File:Test%20image.jpg");
        assertThat(temple.getSourceUrl()).isEqualTo("https://www.wikidata.org/wiki/Q100");
        assertThat(catalog.getPlaces().get(0).getImageUrl()).isNull();
    }

    @Test
    void acceptsAnEmptyCatalogButRejectsAnInvalidProviderResponse() {
        assertThat(mapper.map(json.readTree("{\"results\":{\"bindings\":[]}}"), Instant.EPOCH).getPlaces()).isEmpty();
        assertThatThrownBy(() -> mapper.map(json.readTree("{\"error\":\"timeout\"}"), Instant.EPOCH))
                .isInstanceOf(PlacesUnavailableException.class);
    }
}
