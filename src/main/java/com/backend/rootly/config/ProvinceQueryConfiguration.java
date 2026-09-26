package com.backend.rootly.config;

import com.backend.rootly.enums.ExploreProvince;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class ProvinceQueryConfiguration {
    private final ExploreProperties properties;

    @Bean
    public Map<ExploreProvince, URI> provinceQueryUris(
            @Value("classpath:queries/explore-province.sparql") Resource query) throws IOException {
        String template = query.getContentAsString(StandardCharsets.UTF_8);
        Map<ExploreProvince, URI> uris = new EnumMap<>(ExploreProvince.class);
        for (ExploreProvince province : ExploreProvince.values()) {
            String encoded = URLEncoder.encode(template.replace("__PROVINCE_ID__", province.getWikidataId()),
                    StandardCharsets.UTF_8);
            uris.put(province, URI.create(properties.getWikidataUrl() + "?format=json&query=" + encoded));
        }
        return Map.copyOf(uris);
    }
}
