package com.backend.rootly.controller;

import com.backend.rootly.config.MapperConfiguration;
import com.backend.rootly.domain.CreateQuestionDomain;
import com.backend.rootly.service.QuestionService;
import com.backend.rootly.utility.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class QuestionControllerTests {

    private QuestionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(QuestionService.class);
        ModelMapper modelMapper = new MapperConfiguration().modelMapper();
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(service, modelMapper)).build();
    }

    @Test
    void mapsAValidQuestionUpdateToTheService() throws Exception {
        when(service.updateQuestion(eq("question-1"), any(CreateQuestionDomain.class),
                any(), eq(Locale.ENGLISH)))
                .thenReturn(ResponseEntity.ok(Map.of("responseCode", ResponseCode.QUESTION_UPDATE_SUCCESS)));

        mvc.perform(put("/api/v1/questions/question-1")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"How should visitors enter the temple?",
                                  "body":"I would like to understand the respectful local custom.",
                                  "location":"Kandy",
                                  "category":"rituals-etiquette"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value(ResponseCode.QUESTION_UPDATE_SUCCESS));

        ArgumentCaptor<CreateQuestionDomain> request = ArgumentCaptor.forClass(CreateQuestionDomain.class);
        verify(service).updateQuestion(eq("question-1"), request.capture(), any(), eq(Locale.ENGLISH));
        assertThat(request.getValue().getTitle()).isEqualTo("How should visitors enter the temple?");
        assertThat(request.getValue().getCategory().getSlug()).isEqualTo("rituals-etiquette");
    }

    @Test
    void mapsQuestionAndCommentDeletesToTheService() throws Exception {
        when(service.deleteQuestion(eq("question-1"), any(), eq(Locale.ENGLISH)))
                .thenReturn(ResponseEntity.ok(Map.of("responseCode", ResponseCode.QUESTION_DELETE_SUCCESS)));
        when(service.deleteComment(eq("comment-1"), any(), eq(Locale.ENGLISH)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "responseCode", ResponseCode.QUESTION_COMMENT_DELETE_SUCCESS)));

        mvc.perform(delete("/api/v1/questions/question-1").header("Accept-Language", "en"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/comments/comment-1").header("Accept-Language", "en"))
                .andExpect(status().isOk());

        verify(service).deleteQuestion(eq("question-1"), any(), eq(Locale.ENGLISH));
        verify(service).deleteComment(eq("comment-1"), any(), eq(Locale.ENGLISH));
    }

    @Test
    void mapsAValidCommentUpdateAndRejectsAnEmptyBody() throws Exception {
        when(service.updateComment(eq("comment-1"), eq("Updated answer"),
                any(), eq(Locale.ENGLISH)))
                .thenReturn(ResponseEntity.ok(Map.of(
                        "responseCode", ResponseCode.QUESTION_COMMENT_UPDATE_SUCCESS)));

        mvc.perform(put("/api/v1/comments/comment-1")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Updated answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode")
                        .value(ResponseCode.QUESTION_COMMENT_UPDATE_SUCCESS));
        verify(service).updateComment(eq("comment-1"), eq("Updated answer"), any(), eq(Locale.ENGLISH));

        service = mock(QuestionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(
                service, new MapperConfiguration().modelMapper())).build();
        mvc.perform(put("/api/v1/comments/comment-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
