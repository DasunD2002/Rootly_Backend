package com.backend.rootly.service.impl;

import com.backend.rootly.client.WikidataPlacesClient;
import com.backend.rootly.client.WikipediaPlaceDetailClient;
import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.dto.response.ExploreCategoryResponseDTO;
import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.mapper.WikidataPlaceMapper;
import com.backend.rootly.service.ExplorePlacesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExplorePlacesServiceImplTests {
    private final WikidataPlacesClient client = mock(WikidataPlacesClient.class);
    private final WikipediaPlaceDetailClient detailClient = mock(WikipediaPlaceDetailClient.class);
    private final Clock clock = mock(Clock.class);
    private final Instant fetchedAt = Instant.parse("2026-01-01T00:00:00Z");
    private ExplorePlacesService service;
    private ExploreCatalog catalog;

    @BeforeEach
    void setUp() throws IOException {
        when(clock.instant()).thenReturn(fetchedAt);
        catalog = new WikidataPlaceMapper().map(JsonMapper.builder().build().readTree(
                new ClassPathResource("explore-places-fixture.json").getContentAsString(StandardCharsets.UTF_8)), fetchedAt);
        when(client.fetch()).thenReturn(catalog);
        ExploreProperties properties = new ExploreProperties();
        properties.setCacheTtl(Duration.ofMinutes(10));
        properties.setMaxStale(Duration.ofHours(1));
        service = new ExplorePlacesServiceImpl(client, detailClient, clock, properties);
    }

    private ExplorePlacesResponseDTO search(String query, String category, int page, int size) {
        return (ExplorePlacesResponseDTO) service.search(new ExplorePlacesRequest(query, category, page, size)).getBody();
    }

    @Test
    void searchesFiltersAndPaginatesWithoutInventingFallbackResults() {
        ExplorePlacesResponseDTO first = search("", "all", 0, 1);
        assertThat(first.getTotal()).isEqualTo(2);
        assertThat(first.isHasNext()).isTrue();
        assertThat(first.getItems().get(0).getId()).isEqualTo("Q200");
        assertThat(search("", "all", 1, 1).getItems().get(0).getId()).isEqualTo("Q100");
        assertThat(search("R\u00d3CK temple", "sacred-sites", 0, 20).getTotal()).isEqualTo(1);
        assertThat(search("not-a-real-place", "all", 0, 20).getItems()).isEmpty();
        assertThat(search("", "museums", 0, 20).getItems()).isEmpty();
        assertThat(search("", "all", 1_000_000, 50).getItems()).isEmpty();
        verify(client, times(1)).fetch();
    }

    @Test
    void detailUsesCatalogIdentityAndEnrichesOnlyTheSelectedPlace() {
        ExplorePlaceDTO selected = catalog.getPlaces().get(1);
        when(detailClient.enrich(selected)).thenReturn(selected);
        assertThat(service.getPlace(selected.getId()).getBody()).isEqualTo(selected);
        verify(detailClient).enrich(selected);
        assertThatThrownBy(() -> service.getPlace("bad")).isInstanceOf(IllegalArgumentException.class);
        verify(client, times(1)).fetch();
    }

    @Test
    void categoriesDoNotContactTheProvider() {
        assertThat(service.getCategories()).extracting(ExploreCategoryResponseDTO::getId)
                .containsExactly("ancient-ruins", "sacred-sites", "museums", "heritage-sites");
        assertThat(service.getCategories()).extracting(ExploreCategoryResponseDTO::getLabel)
                .containsExactly("Ancient Ruins", "Sacred Sites", "Museums", "Heritage Sites");
        verifyNoInteractions(client);
    }

    @Test
    void validatesBeforeFetching() {
        assertThatThrownBy(() -> search("x".repeat(121), "all", 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> search("", "all", -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> search("", "all", 0, 51))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> search("", "unknown", 0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void servesStaleRealDataDuringFailureAndStopsServingItAfterTheAgeLimit() {
        assertThat(search("", "all", 0, 20).isStale()).isFalse();
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(601));
        when(client.fetch()).thenThrow(new PlacesUnavailableException("Unavailable", Duration.ofMinutes(5)));
        ExplorePlacesResponseDTO stale = search("", "all", 0, 20);
        assertThat(stale.isStale()).isTrue();
        assertThat(stale.getFetchedAt()).isEqualTo(fetchedAt);
        assertThat(search("", "all", 0, 20).getItems()).hasSize(2);
        verify(client, times(2)).fetch();
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(3601));
        assertThatThrownBy(() -> search("", "all", 0, 20))
                .isInstanceOf(PlacesUnavailableException.class);
    }

    @Test
    void coldFailureBacksOffInsteadOfRepeatedlyCallingTheProvider() {
        when(client.fetch()).thenThrow(new PlacesUnavailableException("Rate limited", Duration.ofMinutes(3)));
        assertThatThrownBy(() -> search("", "all", 0, 20))
                .isInstanceOf(PlacesUnavailableException.class);
        assertThatThrownBy(() -> search("", "all", 0, 20))
                .isInstanceOf(PlacesUnavailableException.class);
        verify(client, times(1)).fetch();
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(181));
        doReturn(catalog).when(client).fetch();
        assertThat(search("", "all", 0, 20).getItems()).hasSize(2);
        verify(client, times(2)).fetch();
    }
}
