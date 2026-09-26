package com.backend.rootly.service.impl;

import com.backend.rootly.dto.response.QuestionDetailDTO;
import com.backend.rootly.entity.Question;
import com.backend.rootly.entity.QuestionComment;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CommentStatus;
import com.backend.rootly.enums.QuestionCategory;
import com.backend.rootly.enums.QuestionStatus;
import com.backend.rootly.repository.ForumVoteRepository;
import com.backend.rootly.repository.QuestionBookmarkRepository;
import com.backend.rootly.repository.QuestionCommentRepository;
import com.backend.rootly.repository.QuestionRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceMutationTests {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final QuestionCommentRepository commentRepository = mock(QuestionCommentRepository.class);
    private final ForumVoteRepository voteRepository = mock(ForumVoteRepository.class);
    private final QuestionBookmarkRepository bookmarkRepository = mock(QuestionBookmarkRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ResponseGenerator responseGenerator = mock(ResponseGenerator.class);
    private QuestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new QuestionServiceImpl(questionRepository, commentRepository, voteRepository,
                bookmarkRepository, userRepository, responseGenerator,
                Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void onlyTheOwnerCanDeleteAQuestion() {
        Question question = question("question-1", "another-user");
        UserReg viewer = user("viewer-1", "Viewer");
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(responseGenerator.generateErrorResponse(isNull(), eq(HttpStatus.FORBIDDEN),
                eq(ResponseCode.FORBIDDEN), any(String.class), eq(Locale.ENGLISH), isNull()))
                .thenReturn(ResponseEntity.status(HttpStatus.FORBIDDEN).build());

        ResponseEntity<Object> response = service.deleteQuestion("question-1", viewer, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(questionRepository, never()).save(question);
    }

    @Test
    void deletingAParentKeepsATombstoneAndItsActiveReplies() {
        UserReg owner = user("owner-1", "Owner User");
        Question question = question("question-1", owner.getId());
        QuestionComment parent = comment("comment-1", null, "comment-1", owner.getId());
        QuestionComment reply = comment("comment-2", "comment-1", "comment-1", "reply-user");

        when(commentRepository.findByIdAndStatus("comment-1", CommentStatus.ACTIVE))
                .thenReturn(Optional.of(parent));
        when(commentRepository.save(any(QuestionComment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(commentRepository.findByQuestionIdOrderByCreatedAtAsc("question-1"))
                .thenReturn(List.of(parent, reply));
        when(questionRepository.findById("question-1")).thenReturn(Optional.of(question));
        when(responseGenerator.generateSuccessResponse(eq(HttpStatus.OK),
                eq(ResponseCode.QUESTION_COMMENT_DELETE_SUCCESS),
                eq(MessageConstant.QUESTION_COMMENT_DELETE_SUCCESS), any()))
                .thenReturn(ResponseEntity.ok().build());

        service.deleteComment("comment-1", owner, Locale.ENGLISH);

        assertThat(parent.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(parent.getBody()).isEqualTo("[deleted]");
        verify(questionRepository, never()).save(question);

        when(userRepository.findAllById(any())).thenReturn(List.of(owner, user("reply-user", "Reply User")));
        when(voteRepository.findByUserIdAndTargetTypeAndTargetIdIn(any(), any(), any()))
                .thenReturn(List.of());
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(bookmarkRepository.findByUserIdAndQuestionId(any(), any())).thenReturn(Optional.empty());
        when(responseGenerator.generateSuccessResponse(eq(HttpStatus.OK),
                eq(ResponseCode.QUESTION_DETAIL_SUCCESS), eq(MessageConstant.QUESTION_DETAIL_SUCCESS), any()))
                .thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(3)));

        ResponseEntity<Object> detailResponse = service.getQuestion(
                "question-1", "top", owner, Locale.ENGLISH);
        QuestionDetailDTO detail = (QuestionDetailDTO) detailResponse.getBody();

        assertThat(detail).isNotNull();
        assertThat(detail.getComments()).hasSize(1);
        assertThat(detail.getComments().get(0).isDeleted()).isTrue();
        assertThat(detail.getComments().get(0).getBody()).isEqualTo("This comment was deleted.");
        assertThat(detail.getComments().get(0).getReplies()).hasSize(1);
        assertThat(detail.getComments().get(0).getReplies().get(0).getId()).isEqualTo("comment-2");
    }

    private static Question question(String id, String authorId) {
        Instant now = Instant.parse("2026-09-25T09:00:00Z");
        return Question.builder()
                .id(id)
                .title("How should visitors enter the temple?")
                .body("I would like to understand the respectful local custom.")
                .location("Kandy")
                .category(QuestionCategory.RITUALS_ETIQUETTE)
                .authorId(authorId)
                .status(QuestionStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static QuestionComment comment(String id, String parentId, String rootId, String authorId) {
        Instant now = Instant.parse("2026-09-25T09:00:00Z");
        return QuestionComment.builder()
                .id(id)
                .questionId("question-1")
                .parentCommentId(parentId)
                .rootCommentId(rootId)
                .authorId(authorId)
                .body("A helpful answer")
                .status(CommentStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static UserReg user(String id, String name) {
        UserReg user = new UserReg();
        user.setId(id);
        user.setName(name);
        user.setRole("USER");
        user.setVerificationStatus("UNVERIFIED");
        return user;
    }
}
