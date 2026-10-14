package com.backend.rootly.controller;

import com.backend.rootly.domain.ProvinceExploreRequest;
import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.dto.response.ProvinceDTO;
import com.backend.rootly.dto.response.ProvinceExploreResponseDTO;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.service.ProvinceExploreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProvinceExploreControllerTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ProvinceExploreService service;

    private static MockHttpServletRequestBuilder province(String body) {
        return post("/api/v1/explore/province").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void publiclyMapsProvinceAndPaginationAndPreservesMetadata() throws Exception {
        ProvinceDTO profile = new ProvinceDTO();
        profile.setId("uva");
        profile.setName("Uva Province");
        profile.setDistricts(List.of("Badulla District"));
        profile.setImageUrl("https://commons.wikimedia.org/wiki/Special:FilePath/Test.jpg");
        var response = new ProvinceExploreResponseDTO(profile, new ExplorePlacesResponseDTO(
                List.of(), 0, 10, 0, false, "Wikidata", Instant.EPOCH, false, false), List.of());
        when(service.explore(new ProvinceExploreRequest("uva", 0, 10))).thenReturn(ResponseEntity.ok(response));
        mvc.perform(province("{\"provinceId\":\"uva\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.province.id").value("uva"))
                .andExpect(jsonPath("$.province.districts[0]").value("Badulla District"))
                .andExpect(jsonPath("$.province.imageUrl").value(profile.getImageUrl()))
                .andExpect(jsonPath("$.places.items").isEmpty())
                .andExpect(jsonPath("$.places.source").value("Wikidata"))
                .andExpect(jsonPath("$.traditions").isEmpty());
        verify(service).explore(new ProvinceExploreRequest("uva", 0, 10));
        when(service.explore(new ProvinceExploreRequest("central", 1, 5))).thenReturn(ResponseEntity.ok(response));
        mvc.perform(province("{\"provinceId\":\"central\",\"page\":1,\"size\":5}")).andExpect(status().isOk());
        verify(service).explore(new ProvinceExploreRequest("central", 1, 5));
    }

    @Test
    void mapsProvinceSearchTextToService() throws Exception {
        ProvinceDTO profile = new ProvinceDTO();
        profile.setId("uva");
        var response = new ProvinceExploreResponseDTO(profile, new ExplorePlacesResponseDTO(
                List.of(), 0, 10, 0, false, "Wikidata", Instant.EPOCH, false, false), List.of());
        when(service.explore(new ProvinceExploreRequest("uva", "temple", 0, 10)))
                .thenReturn(ResponseEntity.ok(response));
        mvc.perform(province("{\"provinceId\":\"uva\",\"q\":\"temple\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.places.total").value(0));
        verify(service).explore(new ProvinceExploreRequest("uva", "temple", 0, 10));
    }

    @Test
    void validatesMissingProvinceAndInvalidPaginationBeforeService() throws Exception {
        for (String body : List.of("{}", "{\"provinceId\":null}", "{\"provinceId\":\"\"}",
                "{\"provinceId\":\"uva\",\"page\":-1}", "{\"provinceId\":\"uva\",\"page\":1000001}",
                "{\"provinceId\":\"uva\",\"page\":null}", "{\"provinceId\":\"uva\",\"size\":null}",
                "{\"provinceId\":\"uva\",\"size\":0}", "{\"provinceId\":\"uva\",\"size\":51}",
                "{\"provinceId\":\"uva\",\"page\":\"invalid\"}", "{\"provinceId\":\"uva\",\"q\":null}",
                "{\"provinceId\":\"uva\",\"q\":\"" + "x".repeat(121) + "\"}", "not-json")) {
            mvc.perform(province(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
        verifyNoInteractions(service);
    }

    @Test
    void returnsProblemDetailsForUnknownProvinceAndProviderFailures() throws Exception {
        when(service.explore(new ProvinceExploreRequest("unknown", 0, 10)))
                .thenThrow(new IllegalArgumentException("Unknown province"));
        mvc.perform(province("{\"provinceId\":\"unknown\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Unknown province"));
        when(service.explore(new ProvinceExploreRequest("uva", 0, 10)))
                .thenThrow(new PlacesUnavailableException("Temporarily unavailable", Duration.ofSeconds(180)));
        mvc.perform(province("{\"provinceId\":\"uva\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "180"));
    }

    @Test
    void permitsConfiguredCorsPostAndKeepsOtherMethodsPrivate() throws Exception {
        mvc.perform(options("/api/v1/explore/province").header("Origin", "http://localhost:54321")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:54321"));
        mvc.perform(options("/api/v1/explore/province").header("Origin", "https://untrusted.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/explore/province")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/explore/province")).andExpect(status().isForbidden());
    }
}

