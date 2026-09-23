package com.backend.rootly.controller;

import com.backend.rootly.domain.ExplorePlacesRequest;
import com.backend.rootly.dto.response.ExploreCategoryResponseDTO;
import com.backend.rootly.dto.response.ExploreLocationDTO;
import com.backend.rootly.dto.response.ExplorePlaceDTO;
import com.backend.rootly.dto.response.ExplorePlacesResponseDTO;
import com.backend.rootly.exception.PlacesUnavailableException;
import com.backend.rootly.service.ExplorePlacesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExplorePlacesControllerTests {
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private ExplorePlacesService service;

    private static MockHttpServletRequestBuilder places(String body) {
        return post("/api/v1/explore/places").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void publicPostReturnsPaginationAndSourceMetadata() throws Exception {
        when(service.search(new ExplorePlacesRequest("", "all", 0, 20))).thenReturn(ResponseEntity.ok(new ExplorePlacesResponseDTO(
                List.of(), 0, 20, 0, false, "Wikidata", Instant.EPOCH, false, false)));
        mvc.perform(places("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.source").value("Wikidata"))
                .andExpect(jsonPath("$.stale").value(false));
        verify(service).search(new ExplorePlacesRequest("", "all", 0, 20));
        when(service.getCategories()).thenReturn(List.of(
                new ExploreCategoryResponseDTO("ancient-ruins", "Ancient Ruins"),
                new ExploreCategoryResponseDTO("sacred-sites", "Sacred Sites"),
                new ExploreCategoryResponseDTO("museums", "Museums"),
                new ExploreCategoryResponseDTO("heritage-sites", "Heritage Sites")));
        mvc.perform(get("/api/v1/explore/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void mapsJsonBodyToDomainRequestAndPreservesNestedResponseFields() throws Exception {
        ExplorePlaceDTO place = new ExplorePlaceDTO("Q100", "Test Temple", "Test District, Sri Lanka",
                "Sacred Sites", "sacred-sites", new ExploreLocationDTO(7.9668, 81.0041),
                null, null, null, "https://www.wikidata.org/wiki/Q100", null);
        when(service.search(new ExplorePlacesRequest("temple", "sacred-sites", 1, 5)))
                .thenReturn(ResponseEntity.ok(new ExplorePlacesResponseDTO(
                        List.of(place), 1, 5, 7, true, "Wikidata", Instant.EPOCH, true, true)));

        mvc.perform(places("""
                        {"q":"temple","category":"sacred-sites","page":1,"size":5}
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "items": [{
                            "id": "Q100",
                            "name": "Test Temple",
                            "subtitle": "Test District, Sri Lanka",
                            "category": "Sacred Sites",
                            "categoryId": "sacred-sites",
                            "location": {"latitude": 7.9668, "longitude": 81.0041},
                            "description": null,
                            "imageUrl": null,
                            "imageSourceUrl": null,
                            "sourceUrl": "https://www.wikidata.org/wiki/Q100",
                            "wikipediaUrl": null
                          }],
                          "page": 1, "size": 5, "total": 7, "hasNext": true,
                          "source": "Wikidata", "fetchedAt": "1970-01-01T00:00:00Z",
                          "stale": true, "truncated": true
                        }
                        """, JsonCompareMode.STRICT));
    }

    @Test
    void publicDetailReturnsTheSelectedPlace() throws Exception {
        ExplorePlaceDTO place = new ExplorePlaceDTO("Q100", "Test Temple", "Sri Lanka",
                "Sacred Sites", "sacred-sites", new ExploreLocationDTO(7.9668, 81.0041),
                "Full historical narrative", "https://upload.wikimedia.org/photo.jpg",
                "https://en.wikipedia.org/wiki/File:photo.jpg",
                "https://www.wikidata.org/wiki/Q100", "https://en.wikipedia.org/wiki/Test_Temple");
        when(service.getPlace("Q100")).thenReturn(ResponseEntity.ok(place));
        mvc.perform(get("/api/v1/explore/places/Q100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Full historical narrative"))
                .andExpect(jsonPath("$.imageUrl").value("https://upload.wikimedia.org/photo.jpg"));
        when(service.getPlace("bad")).thenThrow(new IllegalArgumentException("Invalid place"));
        mvc.perform(get("/api/v1/explore/places/bad")).andExpect(status().isBadRequest());
    }

    @Test
    void reportsBadParametersAndUpstreamFailureAsProblemResponses() throws Exception {
        when(service.search(new ExplorePlacesRequest("", "bad", 0, 20))).thenThrow(new IllegalArgumentException("Invalid category"));
        mvc.perform(places("{\"category\":\"bad\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail").value("Invalid category"));
        mvc.perform(places("{\"page\":\"not-a-number\"}"))
                .andExpect(status().isBadRequest());
        when(service.search(new ExplorePlacesRequest("", "all", 0, 20)))
                .thenThrow(new PlacesUnavailableException("Temporarily unavailable", Duration.ofSeconds(60)));
        mvc.perform(places("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void validatesRequestBodyBeforeCallingService() throws Exception {
        for (String body : List.of("{\"q\":null}", "{\"category\":null}", "{\"page\":-1}",
                "{\"page\":1000001}", "{\"size\":0}", "{\"size\":51}",
                "{\"q\":\"" + "x".repeat(121) + "\"}", "not-json")) {
            mvc.perform(places(body)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
        mvc.perform(post("/api/v1/explore/places").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void allowsPostFromConfiguredOriginsAndRejectsOtherMethodsAndOrigins() throws Exception {
        mvc.perform(options("/api/v1/explore/places").header("Origin", "http://localhost:54321")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:54321"));
        mvc.perform(options("/api/v1/explore/places").header("Origin", "https://untrusted.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/explore/places")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/explore/places")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/explore/categories")).andExpect(status().isForbidden());
    }
}
