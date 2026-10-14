package com.backend.rootly.mapper;

import com.backend.rootly.domain.ProvinceCatalog;
import com.backend.rootly.enums.ExploreProvince;
import com.backend.rootly.exception.PlacesUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WikidataProvinceMapperTests {
    private final JsonMapper json = JsonMapper.builder().build();
    private final WikidataProvinceMapper mapper = new WikidataProvinceMapper(new WikidataPlaceMapper(), json);

    private String fixture() throws IOException {
        return new ClassPathResource("explore-province-fixture.json").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void mapsProfileDistrictsPhotosAndDeduplicatedHeritageAndTraditions() throws IOException {
        ProvinceCatalog result = mapper.map(json.readTree(fixture()), ExploreProvince.UVA, Instant.EPOCH);
        assertThat(result.getProvince().getId()).isEqualTo("uva");
        assertThat(result.getProvince().getCapital()).isEqualTo("Badulla");
        assertThat(result.getProvince().getDistricts()).containsExactly("Badulla District", "Monaragala District");
        assertThat(result.getProvince().getImageUrl()).startsWith("https://commons.wikimedia.org/");
        assertThat(result.getProvince().getImageSourceUrl()).endsWith("File:Test%20Province.jpg");
        assertThat(result.getPlaces().getPlaces()).extracting("id").containsExactly("Q300", "Q100");
        var temple = result.getPlaces().getPlaces().get(1);
        assertThat(temple.getCategoryId()).isEqualTo("sacred-sites");
        assertThat(temple.getImageSourceUrl()).endsWith("File:Test%20Temple.jpg");
        assertThat(temple.getLocation().getLatitude()).isEqualTo(6.98);
        assertThat(temple.getSourceUrl()).isEqualTo("https://www.wikidata.org/wiki/Q100");
        assertThat(result.getPlaces().getPlaces().get(0).getImageUrl()).isNull();
        assertThat(result.getPlaces().getFetchedAt()).isEqualTo(Instant.EPOCH);
        assertThat(result.getTraditions()).hasSize(1);
        assertThat(result.getTraditions().get(0).getDescription()).isEqualTo("Test fixture festival");
        assertThat(result.getTraditions().get(0).getImageSourceUrl()).endsWith("File:Test%20Festival.jpg");
    }

    @Test
    void rejectsWrongProvinceMissingProfileAndMalformedResults() throws IOException {
        String source = fixture();
        assertThatThrownBy(() -> mapper.map(json.readTree(source), ExploreProvince.CENTRAL, Instant.EPOCH))
                .isInstanceOf(PlacesUnavailableException.class);
        assertThatThrownBy(() -> mapper.map(json.readTree("{\"results\":{\"bindings\":[]}}"), ExploreProvince.UVA, Instant.EPOCH))
                .isInstanceOf(PlacesUnavailableException.class);
        assertThatThrownBy(() -> mapper.map(json.readTree("{}"), ExploreProvince.UVA, Instant.EPOCH))
                .isInstanceOf(PlacesUnavailableException.class);
    }

    @Test
    void excludesOriginOnlyPlacesOrdinarySchoolsAndMisleadingTypeSubstrings() throws IOException {
        ProvinceCatalog result = mapper.map(json.readTree(fixture()), ExploreProvince.UVA, Instant.EPOCH);
        assertThat(result.getPlaces().getPlaces()).extracting("id").doesNotContain("Q500", "Q600", "Q700");
    }

    @Test
    void marksProviderRowLimitAndRejectsUntrustedImageHosts() throws IOException {
        var root = json.readTree(fixture());
        var rows = json.createArrayNode();
        for (int i = 0; i < 5000; i++) {
            rows.add(root.path("results").path("bindings").get(0));
        }
        var limited = json.createObjectNode();
        limited.putObject("results").set("bindings", rows);
        assertThat(mapper.map(limited, ExploreProvince.UVA, Instant.EPOCH).getPlaces().isTruncated()).isTrue();
        String untrusted = fixture().replace("commons.wikimedia.org", "commons.wikimedia.org.evil.invalid");
        assertThat(mapper.map(json.readTree(untrusted), ExploreProvince.UVA, Instant.EPOCH)
                .getProvince().getImageUrl()).isNull();
    }
}

