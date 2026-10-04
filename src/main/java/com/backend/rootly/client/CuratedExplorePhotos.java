package com.backend.rootly.client;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reviewed Commons files keyed by the exact Wikidata place, for gaps in P18 metadata. */
@Component
public class CuratedExplorePhotos {
    private final Map<String, String> files = new LinkedHashMap<>();

    public CuratedExplorePhotos(JsonMapper mapper) throws IOException {
        try (InputStream input = CuratedExplorePhotos.class.getResourceAsStream("/explore-photos.json")) {
            if (input == null) throw new IOException("The reviewed Explore photo catalog is missing");
            for (var entry : mapper.readTree(input).path("places")) {
                String id = entry.path("id").asString();
                String title = entry.path("commonsFile").asString();
                if (!id.matches("Q[1-9][0-9]*") || !title.startsWith("File:")) {
                    throw new IOException("Invalid place/photo entry in the reviewed Explore photo catalog");
                }
                files.put(id, title);
            }
        }
    }

    String fileFor(WikimediaPhotoClient.Subject subject) {
        String primary = WikimediaPhotoMetadata.fileTitle(subject.imageUrl());
        return primary == null ? files.get(subject.id()) : primary;
    }
}
