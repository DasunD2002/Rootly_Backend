package com.backend.rootly.client;

import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.dto.response.ProvinceDTO;
import com.backend.rootly.dto.response.ProvinceExploreResponseDTO;
import com.backend.rootly.dto.response.ProvinceTraditionDTO;
import com.backend.rootly.enums.ExploreProvince;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Attaches actual photos without changing province content or place pagination. */
@Component
@RequiredArgsConstructor
public class ProvincePhotoAssembler {
    private final WikimediaPhotoClient photoClient;

    public ProvinceExploreResponseDTO illustrate(ExploreProvince province, ProvinceDTO profile,
                                                  List<ProvinceTraditionDTO> sourceTraditions, ExplorePlacesResponseDTO paged) {
        paged.setItems(photoClient.enrichPlaces(paged.getItems()));
        List<WikimediaPhotoClient.Subject> subjects = new ArrayList<>();
        subjects.add(new WikimediaPhotoClient.Subject(province.getWikidataId(), profile.getName(),
                profile.getImageUrl(), profile.getImageSourceUrl(), profile.getWikipediaUrl()));
        for (ProvinceTraditionDTO tradition : sourceTraditions) {
            subjects.add(new WikimediaPhotoClient.Subject(tradition.getId(), tradition.getName(),
                    tradition.getImageUrl(), tradition.getImageSourceUrl(), tradition.getWikipediaUrl()));
        }
        Map<String, WikimediaPhotoClient.Photo> photos = photoClient.resolve(subjects);
        WikimediaPhotoClient.Photo photo = photos.get(province.getWikidataId());
        ProvinceDTO illustrated = new ProvinceDTO(
                profile.getId(), profile.getName(), profile.getDescription(), profile.getCapital(), profile.getDistricts(),
                photo == null ? null : photo.url(), photo == null ? null : photo.sourceUrl(), profile.getSourceUrl(), profile.getWikipediaUrl());
        List<ProvinceTraditionDTO> traditions = sourceTraditions.stream().map(tradition -> {
            WikimediaPhotoClient.Photo image = photos.get(tradition.getId());
            return new ProvinceTraditionDTO(tradition.getId(), tradition.getName(), tradition.getDescription(),
                    image == null ? null : image.url(), image == null ? null : image.sourceUrl(), tradition.getSourceUrl(), tradition.getWikipediaUrl());
        }).toList();
        return new ProvinceExploreResponseDTO(illustrated, paged, traditions);
    }
}
