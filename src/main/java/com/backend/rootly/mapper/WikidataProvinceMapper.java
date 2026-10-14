package com.backend.rootly.mapper;

import com.backend.rootly.domain.ExploreCatalog;
import com.backend.rootly.domain.ProvinceCatalog;
import com.backend.rootly.dto.response.ProvinceDTO;
import com.backend.rootly.dto.response.ProvinceTraditionDTO;
import com.backend.rootly.enums.ExploreProvince;
import com.backend.rootly.enums.ExploreCategory;
import com.backend.rootly.exception.PlacesUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class WikidataProvinceMapper {
    private static final String FILE_PREFIX = "https://commons.wikimedia.org/wiki/Special:FilePath/";
    private final WikidataPlaceMapper placeMapper;
    private final JsonMapper jsonMapper;

    public ProvinceCatalog map(JsonNode root, ExploreProvince province, Instant fetchedAt) {
        JsonNode bindings = root == null ? null : root.path("results").path("bindings");
        if (bindings == null || !bindings.isArray()) {
            throw unavailable("Wikidata response has no province results array");
        }
        ProvinceDTO profile = new ProvinceDTO();
        profile.setId(province.getId());
        profile.setSourceUrl("https://www.wikidata.org/wiki/" + province.getWikidataId());
        Set<String> districts = new TreeSet<>();
        Map<String, ProvinceTraditionDTO> traditions = new LinkedHashMap<>();
        ArrayNode places = jsonMapper.createArrayNode();
        for (JsonNode row : bindings) {
            collectRow(row, province, profile, districts, traditions, places);
        }
        if (profile.getName() == null) {
            throw unavailable("Wikidata returned no usable province profile");
        }
        profile.setDistricts(List.copyOf(districts));
        ObjectNode placeRoot = jsonMapper.createObjectNode();
        placeRoot.putObject("results").set("bindings", places);
        ExploreCatalog catalog = placeMapper.map(placeRoot, fetchedAt);
        catalog.setTruncated(bindings.size() >= 5000);
        List<ProvinceTraditionDTO> sortedTraditions = new ArrayList<>(traditions.values());
        sortedTraditions.sort(Comparator.comparing(ProvinceTraditionDTO::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ProvinceTraditionDTO::getId));
        return new ProvinceCatalog(profile, catalog, List.copyOf(sortedTraditions));
    }

    private void collectRow(JsonNode row, ExploreProvince province, ProvinceDTO profile, Set<String> districts,
                            Map<String, ProvinceTraditionDTO> traditions, ArrayNode places) {
        if (!province.getWikidataId().equals(entityId(value(row, "province")))) {
            throw unavailable("Wikidata returned data for an unexpected province");
        }
        if (province.getWikidataId().equals(entityId(value(row, "place")))) {
            mergeProfile(profile, districts, row, province);
            return;
        }
        collectRecord(row, places, traditions);
        if ("district of Sri Lanka".equals(value(row, "typeLabel"))
                && province.getWikidataId().equals(entityId(value(row, "area")))) {
            String district = value(row, "name");
            if (district != null) {
                districts.add(district);
            }
        }
    }

    private void collectRecord(JsonNode row, ArrayNode places, Map<String, ProvinceTraditionDTO> traditions) {
        String type = String.valueOf(value(row, "typeLabel")).toLowerCase(Locale.ROOT);
        if (value(row, "heritageStatus") != null || type.contains("festival")
                || type.contains("cultural tradition") || type.contains("traditional dance") || type.contains("handicraft")) {
            mergeTradition(traditions, row);
        }
        ExploreCategory category = classify(type, value(row, "designation") != null);
        if (category != null && value(row, "coordinate") != null
                && "true".equals(value(row, "locatedInProvince"))) {
            ObjectNode place = jsonMapper.createObjectNode();
            row.properties().forEach(entry -> place.set(entry.getKey(), entry.getValue()));
            place.putObject("category").put("value", category.getId());
            places.add(place);
        }
    }

    /** Classifications use provider instance labels/designations, never a place's name or description. */
    private static ExploreCategory classify(String type, boolean designated) {
        if (containsAny(type, List.of("museum"))) {
            return ExploreCategory.MUSEUMS;
        }
        if (containsAny(type, List.of("archaeological", "archeological", "ruins", "ancient city"))) {
            return ExploreCategory.ANCIENT_RUINS;
        }
        if (containsAny(type, List.of("temple", "monastery", "church", "cathedral", "mosque", "stupa", "shrine", "synagogue"))) {
            return ExploreCategory.SACRED_SITES;
        }
        if (designated || containsAny(type, List.of("fort", "fortress", "palace", "heritage site", "historic site", "historical site",
                "monument", "rock relief"))) {
            return ExploreCategory.HERITAGE_SITES;
        }
        return null;
    }

    private static boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(term -> Pattern.compile("\\b" + Pattern.quote(term) + "(?:s|es)?\\b")
                .matcher(value).find());
    }

    private static void mergeProfile(ProvinceDTO profile, Set<String> districts, JsonNode row, ExploreProvince province) {
        if (!province.getWikidataId().equals(entityId(value(row, "place")))) {
            throw unavailable("Wikidata returned a mismatched province profile");
        }
        profile.setName(first(profile.getName(), value(row, "name")));
        profile.setDescription(first(profile.getDescription(), value(row, "description")));
        profile.setCapital(first(profile.getCapital(), value(row, "capitalLabel")));
        profile.setWikipediaUrl(first(profile.getWikipediaUrl(), secureUrl(value(row, "article"), "en.wikipedia.org")));
        String district = value(row, "districtLabel");
        if (district != null) {
            districts.add(district);
        }
        if (profile.getImageUrl() == null) {
            String image = secureUrl(value(row, "image"), "commons.wikimedia.org");
            profile.setImageUrl(image);
            profile.setImageSourceUrl(imageSource(image));
        }
    }

    private static void mergeTradition(Map<String, ProvinceTraditionDTO> traditions, JsonNode row) {
        String id = entityId(value(row, "place"));
        String name = value(row, "name");
        if (id == null || name == null) {
            return;
        }
        String image = secureUrl(value(row, "image"), "commons.wikimedia.org");
        ProvinceTraditionDTO next = new ProvinceTraditionDTO(id, name, value(row, "description"),
                image, imageSource(image), "https://www.wikidata.org/wiki/" + id,
                secureUrl(value(row, "article"), "en.wikipedia.org"));
        traditions.merge(id, next, (first, second) -> {
            first.setDescription(first(first.getDescription(), second.getDescription()));
            first.setWikipediaUrl(first(first.getWikipediaUrl(), second.getWikipediaUrl()));
            if (first.getImageUrl() == null) {
                first.setImageUrl(second.getImageUrl());
                first.setImageSourceUrl(second.getImageSourceUrl());
            }
            return first;
        });
    }

    private static String first(String current, String next) {
        return current == null ? next : current;
    }

    private static String value(JsonNode row, String field) {
        JsonNode node = row.path(field).path("value");
        return node.isString() && !node.asString().isBlank() ? node.asString().trim() : null;
    }

    private static String entityId(String uri) {
        return uri != null && uri.matches("https?://www\\.wikidata\\.org/entity/Q[1-9][0-9]*")
                ? uri.substring(uri.lastIndexOf('/') + 1) : null;
    }

    private static String secureUrl(String value, String host) {
        if (value == null) {
            return null;
        }
        String https = value.replaceFirst("^http://", "https://");
        return https.startsWith("https://" + host + "/") ? https : null;
    }

    private static String imageSource(String image) {
        return image != null && image.startsWith(FILE_PREFIX)
                ? "https://commons.wikimedia.org/wiki/File:" + image.substring(FILE_PREFIX.length()) : null;
    }

    private static PlacesUnavailableException unavailable(String message) {
        return new PlacesUnavailableException(message, Duration.ofSeconds(60));
    }
}
