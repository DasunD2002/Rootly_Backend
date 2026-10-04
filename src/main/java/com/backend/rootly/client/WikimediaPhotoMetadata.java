package com.backend.rootly.client;

import com.backend.rootly.client.WikimediaPhotoClient.Photo;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Validates Wikimedia URLs and matches candidate files to their subject. */
final class WikimediaPhotoMetadata {
    private static final Set<String> IMAGE_HOSTS = Set.of("upload.wikimedia.org", "thumb.wikimedia.org");
    private static final Set<String> GENERIC_WORDS = Set.of(
            "the", "of", "in", "sri", "lanka", "raja", "maha", "vihara", "viharaya", "temple", "fort", "church");

    private WikimediaPhotoMetadata() { }

    static Photo readPhoto(JsonNode page) {
        JsonNode info = page.path("imageinfo");
        if (!info.isArray() || info.isEmpty()) return null;
        JsonNode image = info.get(0);
        String url = text(image.path("thumburl"));
        if (url == null) url = text(image.path("url"));
        String mime = text(image.path("thumbmime"));
        if (mime == null) mime = text(image.path("mime"));
        return trustedImage(url) && Set.of("image/jpeg", "image/png", "image/webp", "image/gif").contains(mime == null ? "" : mime)
                ? new Photo(url, text(image.path("descriptionurl"))) : null;
    }

    static boolean trustedImage(String url) {
        if (url == null) return false;
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && IMAGE_HOSTS.contains(uri.getHost()) && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getPath() != null && uri.getPath().startsWith("/wikipedia/");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    static String fileTitle(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            String prefix = "/wiki/Special:FilePath/";
            if ("commons.wikimedia.org".equals(uri.getHost()) && uri.getRawPath().startsWith(prefix)) {
                return "File:" + decode(uri.getRawPath().substring(prefix.length())).replace('_', ' ');
            }
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return null;
    }

    static String articleTitle(String url) {
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            if ("https".equals(uri.getScheme()) && "en.wikipedia.org".equals(uri.getHost()) && uri.getRawPath().startsWith("/wiki/")) {
                return decode(uri.getRawPath().substring(6)).replace('_', ' ');
            }
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return null;
    }

    static boolean matchesSubjectFile(String name, String title) {
        if (name == null || title == null || !title.toLowerCase(Locale.ROOT).matches(".*\\.(jpg|jpeg|png|webp)$")) return false;
        List<String> words = List.of(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().split(" +"))
                .stream().filter(word -> word.length() > 3 && !GENERIC_WORDS.contains(word)).toList();
        String normalizedTitle = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ");
        return !words.isEmpty() && words.stream().allMatch(word -> normalizedTitle.contains(" " + word + " "));
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    static String text(JsonNode node) {
        return node.isString() && !node.asString().isBlank() ? node.asString() : null;
    }

    static void copyAliases(JsonNode aliases, Map<String, Photo> photos) {
        for (JsonNode alias : aliases) {
            String from = text(alias.path("from"));
            Photo photo = photos.get(text(alias.path("to")));
            if (from != null && photo != null) photos.put(from, photo);
        }
    }

}
