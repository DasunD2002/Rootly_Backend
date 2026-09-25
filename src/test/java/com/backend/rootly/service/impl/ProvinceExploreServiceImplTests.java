package com.backend.rootly.service.impl;

import com.backend.rootly.client.WikidataProvinceClient;
import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ProvinceCatalog;
import com.backend.rootly.domain.ProvinceExploreRequest;
import com.backend.rootly.dto.response.ProvinceExploreResponseDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.enums.ExploreProvince;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.mapper.WikidataPlaceMapper;
import com.backend.rootly.mapper.WikidataProvinceMapper;
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

class ProvinceExploreServiceImplTests {
    private final WikidataProvinceClient client = mock(WikidataProvinceClient.class);
    private final Clock clock = mock(Clock.class);
    private final Instant fetchedAt = Instant.parse("2026-01-01T00:00:00Z");
    private ProvinceExploreServiceImpl service;
    private ProvinceCatalog catalog;

    @BeforeEach
    void setUp() throws IOException {
        when(clock.instant()).thenReturn(fetchedAt);
        JsonMapper json = JsonMapper.builder().build();
        catalog = new WikidataProvinceMapper(new WikidataPlaceMapper(), json).map(json.readTree(
                new ClassPathResource("explore-province-fixture.json").getContentAsString(StandardCharsets.UTF_8)),
                ExploreProvince.UVA, fetchedAt);
        when(client.fetch(ExploreProvince.UVA)).thenReturn(catalog);
        ExploreProperties properties = new ExploreProperties();
        properties.setCacheTtl(Duration.ofMinutes(10));
        properties.setMaxStale(Duration.ofHours(1));
        service = new ProvinceExploreServiceImpl(client, clock, properties);
    }

    private ProvinceExploreResponseDTO explore(String province, int page, int size) {
        return (ProvinceExploreResponseDTO) service.explore(new ProvinceExploreRequest(province, page, size)).getBody();
    }

    @Test
    void paginatesFromOneCachedCatalogAndDoesNotMixProvinces() {
        var first = explore("uva", 0, 1);
        assertThat(first.getPlaces().getTotal()).isEqualTo(2);
        assertThat(first.getPlaces().isHasNext()).isTrue();
        assertThat(first.getPlaces().getItems().get(0).getId()).isEqualTo("Q300");
        assertThat(explore("uva", 1, 1).getPlaces().getItems().get(0).getId()).isEqualTo("Q100");
        assertThat(explore("uva", 1_000_000, 50).getPlaces().getItems()).isEmpty();
        verify(client, times(1)).fetch(ExploreProvince.UVA);
        when(client.fetch(ExploreProvince.CENTRAL))
                .thenThrow(new PlacesUnavailableException("Unavailable", Duration.ofMinutes(3)));
        assertThatThrownBy(() -> explore("central", 0, 10)).isInstanceOf(PlacesUnavailableException.class);
        assertThat(explore("uva", 0, 10).getProvince().getId()).isEqualTo("uva");
    }

    @Test
    void searchesEveryCachedPlaceBeforePaginationWithoutChangingTraditions() {
        var result = (ProvinceExploreResponseDTO) service.explore(
                new ProvinceExploreRequest("uva", "  TEMPLE  ", 0, 1)).getBody();
        assertThat(result.getPlaces().getTotal()).isEqualTo(1);
        assertThat(result.getPlaces().getItems()).extracting(ExplorePlaceDTO::getId).containsExactly("Q100");
        assertThat(result.getPlaces().isHasNext()).isFalse();
        assertThat(result.getTraditions()).hasSize(1);
        var noMatch = (ProvinceExploreResponseDTO) service.explore(
                new ProvinceExploreRequest("uva", "unknown site", 0, 10)).getBody();
        assertThat(noMatch.getPlaces().getTotal()).isZero();
        assertThat(noMatch.getPlaces().getItems()).isEmpty();
        verify(client, times(1)).fetch(ExploreProvince.UVA);
    }

    @Test
    void validatesBeforeFetching() {
        for (String id : new String[]{"unknown", "", "Q876293", "uva UNION"}) {
            assertThatThrownBy(() -> explore(id, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> explore("uva", -1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> explore("uva", 0, 51)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.explore(
                new ProvinceExploreRequest("uva", "x".repeat(121), 0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void servesStaleRealDataDuringBackoffAndStopsAfterAgeLimit() {
        assertThat(explore("uva", 0, 10).getPlaces().isStale()).isFalse();
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(601));
        when(client.fetch(ExploreProvince.UVA))
                .thenThrow(new PlacesUnavailableException("Unavailable", Duration.ofMinutes(5)));
        var stale = explore("uva", 0, 10).getPlaces();
        assertThat(stale.isStale()).isTrue();
        assertThat(stale.getFetchedAt()).isEqualTo(fetchedAt);
        assertThat(explore("uva", 0, 10).getPlaces().getItems()).hasSize(2);
        verify(client, times(2)).fetch(ExploreProvince.UVA);
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(3601));
        assertThatThrownBy(() -> explore("uva", 0, 10)).isInstanceOf(PlacesUnavailableException.class);
    }

    @Test
    void coldFailureBacksOffThenRecoversWithoutSampleData() {
        when(client.fetch(ExploreProvince.UVA))
                .thenThrow(new PlacesUnavailableException("Rate limited", Duration.ofMinutes(3)));
        assertThatThrownBy(() -> explore("uva", 0, 10)).isInstanceOf(PlacesUnavailableException.class);
        assertThatThrownBy(() -> explore("uva", 0, 10)).isInstanceOf(PlacesUnavailableException.class);
        verify(client, times(1)).fetch(ExploreProvince.UVA);
        when(clock.instant()).thenReturn(fetchedAt.plusSeconds(181));
        doReturn(catalog).when(client).fetch(ExploreProvince.UVA);
        assertThat(explore("uva", 0, 10).getPlaces().getItems()).hasSize(2);
        verify(client, times(2)).fetch(ExploreProvince.UVA);
    }
}

