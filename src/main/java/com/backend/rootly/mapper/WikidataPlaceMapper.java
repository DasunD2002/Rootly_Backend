package com.backend.rootly.mapper;

import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.dto.response.ExploreLocationDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.enums.ExploreCategory;
import com.backend.rootly.exception.PlacesUnavailableException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WikidataPlaceMapper {

    private static final Pattern COORDINATES = Pattern.compile(
            "Point\\(\\s*([-+0-9.eE]+)\\s+([-+0-9.eE]+)\\s*\\)");
    private static final String FILE_PREFIX = "https://commons.wikimedia.org/wiki/Special:FilePath/";

    public ExploreCatalog map(JsonNode root, Instant fetchedAt) {
        JsonNode bindings = bindings(root);
        Map<String, ExplorePlaceDTO> places = new LinkedHashMap<>();
        for (JsonNode binding : bindings) {
            ExplorePlaceDTO place = mapPlace(binding);
            if (place != null) {
                places.merge(place.getId(), place, WikidataPlaceMapper::merge);
            }
        }
        if (!bindings.isEmpty() && places.isEmpty()) {
            throw new PlacesUnavailableException("Wikidata returned no usable place records", Duration.ofSeconds(60));
        }
        return new ExploreCatalog(places.values().stream()
                .sorted(Comparator.comparing(ExplorePlaceDTO::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ExplorePlaceDTO::getId)).toList(), fetchedAt, bindings.size() >= 5000);
    }

    private ExplorePlaceDTO mapPlace(JsonNode binding) {
        String id = entityId(value(binding, "place"));
        String name = value(binding, "name");
        ExploreLocationDTO location = location(value(binding, "coordinate"));
        ExploreCategory category = category(binding);
        if (id == null || name == null || location == null || category == null) {
            return null;
        }
        String area = value(binding, "areaLabel");
        String image = secureUrl(value(binding, "image"), "commons.wikimedia.org");
        String imageSource = image != null && image.startsWith(FILE_PREFIX)
                ? "https://commons.wikimedia.org/wiki/File:" + image.substring(FILE_PREFIX.length()) : null;
        return new ExplorePlaceDTO(id, name, area == null ? "Sri Lanka" : area + ", Sri Lanka",
                category.getLabel(), category.getId(), location, value(binding, "description"), image,
                imageSource, "https://www.wikidata.org/wiki/" + id,
                secureUrl(value(binding, "article"), "en.wikipedia.org"));
    }

    private static JsonNode bindings(JsonNode root) {
        JsonNode bindings = root == null ? null : root.path("results").path("bindings");
        if (bindings == null || !bindings.isArray()) {
            throw new PlacesUnavailableException("Wikidata response has no results array", Duration.ofSeconds(60));
        }
        return bindings;
    }

    private static String entityId(String entity) {
        return entity != null && entity.matches("https?://www\\.wikidata\\.org/entity/Q[1-9][0-9]*")
                ? entity.substring(entity.lastIndexOf('/') + 1) : null;
    }

    private static ExploreCategory category(JsonNode binding) {
        try {
            String categoryId = value(binding, "category");
            return categoryId == null ? null : ExploreCategory.fromId(categoryId);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static ExplorePlaceDTO merge(ExplorePlaceDTO first, ExplorePlaceDTO next) {
        ExploreCategory firstCategory = ExploreCategory.fromId(first.getCategoryId());
        ExploreCategory nextCategory = ExploreCategory.fromId(next.getCategoryId());
        ExploreCategory category = firstCategory.ordinal() <= nextCategory.ordinal() ? firstCategory : nextCategory;
        boolean useNextImage = first.getImageUrl() == null;
        return new ExplorePlaceDTO(first.getId(), first.getName(), first.getSubtitle(), category.getLabel(), category.getId(),
                first.getLocation(), first.getDescription() == null ? next.getDescription() : first.getDescription(),
                useNextImage ? next.getImageUrl() : first.getImageUrl(),
                useNextImage ? next.getImageSourceUrl() : first.getImageSourceUrl(), first.getSourceUrl(),
                first.getWikipediaUrl() == null ? next.getWikipediaUrl() : first.getWikipediaUrl());
    }

    private static String value(JsonNode binding, String field) {
        JsonNode node = binding.path(field).path("value");
        return node.isString() && !node.asString().isBlank() ? node.asString().trim() : null;
    }

    private static String secureUrl(String value, String host) {
        if (value == null) {
            return null;
        }
        String https = value.replaceFirst("^http://", "https://");
        return https.startsWith("https://" + host + "/") ? https : null;
    }

    private static ExploreLocationDTO location(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = COORDINATES.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        try {
            double longitude = Double.parseDouble(matcher.group(1));
            double latitude = Double.parseDouble(matcher.group(2));
            // Reject incorrectly located source records outside Sri Lanka's bounding region.
            return latitude >= 5.5 && latitude <= 10.1 && longitude >= 79.0 && longitude <= 82.1
                    ? new ExploreLocationDTO(latitude, longitude) : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
