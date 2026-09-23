package com.backend.rootly.service.impl;

import com.backend.rootly.client.WikidataProvinceClient;
import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.domain.ProvinceCatalog;
import com.backend.rootly.domain.ProvinceExploreRequest;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.dto.response.ProvinceExploreResponseDTO;
import com.backend.rootly.enums.ExploreProvince;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.service.ProvinceExploreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
@RequiredArgsConstructor
public class ProvinceExploreServiceImpl implements ProvinceExploreService {
    private final WikidataProvinceClient client;
    private final Clock clock;
    private final ExploreProperties properties;
    private final Map<ExploreProvince, ProvinceCatalog> cached = new EnumMap<>(ExploreProvince.class);
    private final Map<ExploreProvince, Instant> nextAttempts = new EnumMap<>(ExploreProvince.class);

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public ResponseEntity<Object> explore(ProvinceExploreRequest request) {
        ExploreProvince province = ExploreProvince.fromId(request.getProvinceId());
        validatePage(request.getPage(), request.getSize());
        String query = request.getQ();
        if (query == null || query.length() > 120) {
            throw new IllegalArgumentException("q must be at most 120 characters");
        }
        int page = request.getPage();
        int size = request.getSize();
        ProvinceCatalog result = catalog(province);
        ExploreCatalog places = result.getPlaces();
        List<ExplorePlaceDTO> matches = filterPlaces(places.getPlaces(), query);
        long start = (long) page * size;
        List<ExplorePlaceDTO> items = matches.stream().skip(start).limit(size).toList();
        ExplorePlacesResponseDTO paged = new ExplorePlacesResponseDTO(items, page, size,
                matches.size(), start + items.size() < matches.size(),
                "Wikidata", places.getFetchedAt(), !withinAge(result, properties.getCacheTtl()), places.isTruncated());
        return ResponseEntity.ok(new ProvinceExploreResponseDTO(result.getProvince(), paged, result.getTraditions()));
    }

    private static List<ExplorePlaceDTO> filterPlaces(List<ExplorePlaceDTO> places, String query) {
        List<String> words = Arrays.stream(normalize(query).split("\\s+"))
                .filter(word -> !word.isEmpty()).toList();
        if (words.isEmpty()) {
            return places;
        }
        return places.stream().filter(place -> {
            String searchable = normalize(place.getName() + " " + place.getSubtitle() + " "
                    + place.getCategory() + " " + (place.getDescription() == null ? "" : place.getDescription()));
            return words.stream().allMatch(searchable::contains);
        }).toList();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .trim().toLowerCase(Locale.ROOT);
    }

    private synchronized ProvinceCatalog catalog(ExploreProvince province) {
        ProvinceCatalog current = cached.get(province);
        if (current != null && withinAge(current, properties.getCacheTtl())) {
            return current;
        }
        Instant nextAttempt = nextAttempts.getOrDefault(province, Instant.MIN);
        if (!clock.instant().isBefore(nextAttempt)) {
            try {
                ProvinceCatalog refreshed = client.fetch(province);
                cached.put(province, refreshed);
                nextAttempts.remove(province);
                return refreshed;
            } catch (PlacesUnavailableException exception) {
                nextAttempt = clock.instant().plus(exception.getRetryAfter());
                nextAttempts.put(province, nextAttempt);
                if (log.isWarnEnabled()) {
                    log.warn("Could not refresh province {}: {}", province.getId(), exception.getMessage());
                }
            }
        }
        if (current != null && withinAge(current, properties.getMaxStale())) {
            return current;
        }
        throw new PlacesUnavailableException("Real province data is temporarily unavailable. Please retry shortly.",
                Duration.between(clock.instant(), nextAttempt));
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    private boolean withinAge(ProvinceCatalog catalog, Duration maxAge) {
        return clock.instant().isBefore(catalog.getPlaces().getFetchedAt().plus(maxAge));
    }

    private static void validatePage(Integer page, Integer size) {
        if (page == null || size == null || page < 0 || page > 1_000_000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page must be 0..1000000 and size must be 1..50");
        }
    }
}
