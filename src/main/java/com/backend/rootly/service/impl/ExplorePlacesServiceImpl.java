package com.backend.rootly.service.impl;

import com.backend.rootly.client.WikidataPlacesClient;
import com.backend.rootly.client.WikipediaPlaceDetailClient;
import com.backend.rootly.config.ExploreProperties;
import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.dto.response.ExploreCategoryResponseDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.enums.ExploreCategory;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.service.ExplorePlacesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class ExplorePlacesServiceImpl implements ExplorePlacesService {

    private static final int MAX_QUERY_LENGTH = 120;
    private static final int MAX_PAGE = 1_000_000;
    private static final int MAX_PAGE_SIZE = 50;
    private final WikidataPlacesClient client;
    private final WikipediaPlaceDetailClient detailClient;
    private final Clock clock;
    private final ExploreProperties properties;
    private ExploreCatalog cached;
    private Instant nextAttempt = Instant.MIN;

    @Override
    public ResponseEntity<Object> search(ExplorePlacesRequest request) {
        String query = request.getQ();
        String categoryId = request.getCategory();
        int page = request.getPage();
        int size = request.getSize();
        validate(query, page, size);

        ExploreCategory category = categoryId.isBlank() || "all".equalsIgnoreCase(categoryId.trim())
                ? null : ExploreCategory.fromId(categoryId);

        List<String> words = Arrays.stream(normalize(query).split("\\s+"))
                .filter(word -> !word.isEmpty()).toList();

        ExploreCatalog catalog = catalog();

        List<ExplorePlaceDTO> matches = catalog.getPlaces().stream()
                .filter(place -> category == null || category.getId().equals(place.getCategoryId()))
                .filter(place -> matches(place, words)).toList();

        long start = (long) page * size;

        List<ExplorePlaceDTO> items = matches.stream().skip(start).limit(size).toList();

        return ResponseEntity.ok(new ExplorePlacesResponseDTO(items, page, size, matches.size(), start + items.size() < matches.size(),
                "Wikidata", catalog.getFetchedAt(), !isFresh(catalog), catalog.isTruncated()));
    }

    @Override
    public ResponseEntity<Object> getPlace(String placeId) {
        if (placeId == null || !placeId.matches("Q[1-9][0-9]*")) {
            throw new IllegalArgumentException("placeId must be a Wikidata Q identifier");
        }
        ExplorePlaceDTO place = catalog().getPlaces().stream()
                .filter(item -> placeId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place was not found"));
        return ResponseEntity.ok(detailClient.enrich(place));
    }

    @Override
    public List<ExploreCategoryResponseDTO> getCategories() {
        return Arrays.stream(ExploreCategory.values())
                .map(category -> new ExploreCategoryResponseDTO(category.getId(), category.getLabel()))
                .toList();
    }

    /** One shared refresh avoids sending a public upstream request for every search. */
    private synchronized ExploreCatalog catalog() {
        if (cached != null && isFresh(cached)) {
            return cached;
        }
        if (!clock.instant().isBefore(nextAttempt)) {
            try {
                cached = client.fetch();
                nextAttempt = Instant.MIN;
                return cached;
            } catch (PlacesUnavailableException exception) {
                nextAttempt = clock.instant().plus(exception.getRetryAfter());
                if (log.isWarnEnabled()) {
                    log.warn("Could not refresh Explore Places: {}", exception.getMessage());
                }
            }
        }
        if (cached != null && isWithinAge(cached.getFetchedAt(), properties.getMaxStale())) {
            return cached;
        }
        throw new PlacesUnavailableException("Real place data is temporarily unavailable. Please retry shortly.",
                Duration.between(clock.instant(), nextAttempt));
    }

    private boolean isFresh(ExploreCatalog catalog) {
        return isWithinAge(catalog.getFetchedAt(), properties.getCacheTtl());
    }

    private boolean isWithinAge(Instant fetchedAt, Duration maxAge) {
        return clock.instant().isBefore(fetchedAt.plus(maxAge));
    }

    private static void validate(String query, int page, int size) {
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q must be at most 120 characters");
        }
        if (page < 0 || page > MAX_PAGE || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page must be 0..1000000 and size must be 1..50");
        }
    }

    private static boolean matches(ExplorePlaceDTO place, List<String> words) {
        String text = normalize(place.getName() + " " + place.getSubtitle() + " " + place.getCategory()
                + " " + (place.getDescription() == null ? "" : place.getDescription()));
        return words.stream().allMatch(text::contains);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .trim().toLowerCase(Locale.ROOT);
    }
}
