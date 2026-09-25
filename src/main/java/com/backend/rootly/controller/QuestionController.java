package com.backend.rootly.controller;

import com.backend.rootly.domain.CreateQuestionCommentDomain;
import com.backend.rootly.domain.CreateQuestionDomain;
import com.backend.rootly.domain.ForumVoteDomain;
import com.backend.rootly.domain.QuestionSearchDomain;
import com.backend.rootly.dto.request.CreateQuestionCommentRequestDTO;
import com.backend.rootly.dto.request.CreateQuestionRequestDTO;
import com.backend.rootly.dto.request.ForumVoteRequestDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.QuestionCategory;
import com.backend.rootly.service.QuestionService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
@Log4j2
public class QuestionController {

    private final QuestionService questionService;
    private final ModelMapper modelMapper;

    @GetMapping(value = EndPoint.QUESTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getQuestions(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "top") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserReg viewer,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        QuestionSearchDomain search = QuestionSearchDomain.builder()
                .query(query)
                .category(category == null || category.isBlank() ? null : QuestionCategory.fromValue(category))
                .sort(sort)
                .page(page)
                .size(size)
                .build();
        return questionService.getQuestions(search, viewer, locale);
    }

    @GetMapping(value = EndPoint.QUESTION_DETAIL, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getQuestion(
            @PathVariable String questionId,
            @RequestParam(defaultValue = "top") String commentSort,
            @AuthenticationPrincipal UserReg viewer,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return questionService.getQuestion(questionId, commentSort, viewer, locale);
    }

    @PostMapping(value = EndPoint.QUESTIONS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createQuestion(
            @Validated @RequestBody CreateQuestionRequestDTO requestDTO,
            @AuthenticationPrincipal UserReg author,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        CreateQuestionDomain request = modelMapper.map(requestDTO, CreateQuestionDomain.class);
        return questionService.createQuestion(request, author, locale);
    }

    @PostMapping(value = EndPoint.QUESTION_COMMENTS, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createComment(
            @PathVariable String questionId,
            @Validated @RequestBody CreateQuestionCommentRequestDTO requestDTO,
            @AuthenticationPrincipal UserReg author,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        CreateQuestionCommentDomain request = modelMapper.map(requestDTO, CreateQuestionCommentDomain.class);
        return questionService.createComment(questionId, request, author, locale);
    }

    @PutMapping(value = EndPoint.QUESTION_VOTE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> voteQuestion(
            @PathVariable String questionId,
            @Validated @RequestBody ForumVoteRequestDTO requestDTO,
            @AuthenticationPrincipal UserReg voter,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        ForumVoteDomain request = modelMapper.map(requestDTO, ForumVoteDomain.class);
        return questionService.voteQuestion(questionId, request, voter, locale);
    }

    @PutMapping(value = EndPoint.COMMENT_VOTE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> voteComment(
            @PathVariable String commentId,
            @Validated @RequestBody ForumVoteRequestDTO requestDTO,
            @AuthenticationPrincipal UserReg voter,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        ForumVoteDomain request = modelMapper.map(requestDTO, ForumVoteDomain.class);
        return questionService.voteComment(commentId, request, voter, locale);
    }

    @PutMapping(value = EndPoint.QUESTION_BOOKMARK, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> bookmarkQuestion(
            @PathVariable String questionId,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return questionService.bookmarkQuestion(questionId, user, locale);
    }

    @DeleteMapping(value = EndPoint.QUESTION_BOOKMARK, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> removeQuestionBookmark(
            @PathVariable String questionId,
            @AuthenticationPrincipal UserReg user,
            @RequestHeader(value = "Accept-Language", required = false) Locale locale) {
        return questionService.removeQuestionBookmark(questionId, user, locale);
    }
}
