package com.backend.rootly.service.impl;

import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.request.SubmitQuizAnswerRequestDTO;
import com.backend.rootly.dto.response.QuizAnswerResponseDTO;
import com.backend.rootly.dto.response.QuizSessionResponseDTO;
import com.backend.rootly.entity.QuizQuestion;
import com.backend.rootly.entity.QuizSession;
import com.backend.rootly.entity.QuizStageAttempt;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.QuizSessionStatus;
import com.backend.rootly.enums.QuizType;
import com.backend.rootly.repository.QuizQuestionRepository;
import com.backend.rootly.repository.QuizSessionRepository;
import com.backend.rootly.service.QuizService;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.support.StaticMessageSource;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizServiceImplTests {

    private final QuizQuestionRepository questionRepository = mock(QuizQuestionRepository.class);
    private final QuizSessionRepository sessionRepository = mock(QuizSessionRepository.class);
    private QuizService service;
    private UserReg user;

    @BeforeEach
    void setUp() {
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("val.quiz.session.success", Locale.ENGLISH, "Quiz session retrieved successfully");
        messages.addMessage("val.quiz.answer.success", Locale.ENGLISH, "Quiz answer processed successfully");
        messages.addMessage("val.quiz.dashboard.success", Locale.ENGLISH, "Quiz dashboard retrieved successfully");
        service = new QuizServiceImpl(questionRepository, sessionRepository,
                new ResponseGenerator(new ModelMapper(), messages));
        user = new UserReg();
        user.setId("user-1");
        user.setName("Amaya Perera");
    }

    @Test
    void startsAnAuthenticatedSessionWithoutExposingAnswers() {
        when(sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(
                "user-1", QuizSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(sessionRepository.findByUserIdOrderByStartedAtDesc("user-1")).thenReturn(List.of());
        stubQuestionPools();
        when(sessionRepository.save(any(QuizSession.class))).thenAnswer(invocation -> {
            QuizSession session = invocation.getArgument(0);
            session.setId("session-1");
            return session;
        });

        ResponseDTO envelope = (ResponseDTO) service.startOrResume(user, Locale.ENGLISH).getBody();
        QuizSessionResponseDTO result = (QuizSessionResponseDTO) envelope.getData();

        assertThat(result.getSessionId()).isEqualTo("session-1");
        assertThat(result.getQuestion().getType()).isEqualTo(QuizType.WORD_BUILDER);
        assertThat(result.getQuestion().getLetters()).containsExactlyInAnyOrder("S", "T", "U", "P", "A");
        assertThat(result.getQuestion().getDeadlineAt()).isAfter(Instant.now());
    }

    @Test
    void excludesQuestionsPreviouslyServedToTheUser() {
        QuizQuestion previous = question("builder-old", "OLD", QuizType.WORD_BUILDER, 40);
        QuizQuestion next = question("builder-new", "NEW", QuizType.WORD_BUILDER, 40);
        QuizSession history = QuizSession.builder()
                .userId("user-1")
                .status(QuizSessionStatus.COMPLETED)
                .startedAt(Instant.now().minusSeconds(60))
                .stages(List.of(QuizStageAttempt.builder()
                        .questionId(previous.getId()).type(QuizType.WORD_BUILDER).build()))
                .build();
        when(sessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(
                "user-1", QuizSessionStatus.IN_PROGRESS)).thenReturn(Optional.empty());
        when(sessionRepository.findByUserIdOrderByStartedAtDesc("user-1")).thenReturn(List.of(history));
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.WORD_BUILDER))
                .thenReturn(List.of(previous, next));
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.MEANING_MATCH))
                .thenReturn(List.of(question("meaning-1", "Meaning", QuizType.MEANING_MATCH, 30)));
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.FILL_LETTERS))
                .thenReturn(List.of(question("fill-1", "XY", QuizType.FILL_LETTERS, 30)));
        when(sessionRepository.save(any(QuizSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseDTO envelope = (ResponseDTO) service.startOrResume(user, Locale.ENGLISH).getBody();
        QuizSessionResponseDTO result = (QuizSessionResponseDTO) envelope.getData();

        assertThat(result.getQuestion().getId()).isEqualTo("builder-new");
    }

    @Test
    void validatesAnAnswerAgainstTheUsersCurrentSession() {
        QuizSession session = activeSession();
        when(sessionRepository.findByIdAndUserId("session-1", "user-1"))
                .thenReturn(Optional.of(session));
        when(sessionRepository.save(any(QuizSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseDTO envelope = (ResponseDTO) service.submitAnswer(
                "session-1", new SubmitQuizAnswerRequestDTO("builder-1", "STUPA", false),
                user, Locale.ENGLISH).getBody();
        QuizAnswerResponseDTO result = (QuizAnswerResponseDTO) envelope.getData();

        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getPointsAwarded()).isBetween(28, 40);
        assertThat(result.getSession().getCurrentStage()).isEqualTo(1);
        assertThat(result.getSession().getQuestion().getType()).isEqualTo(QuizType.MEANING_MATCH);
        verify(sessionRepository).findByIdAndUserId("session-1", "user-1");
    }

    private void stubQuestionPools() {
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.WORD_BUILDER))
                .thenReturn(List.of(question("builder-1", "STUPA", QuizType.WORD_BUILDER, 40)));
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.MEANING_MATCH))
                .thenReturn(List.of(question("meaning-1", "Meaning", QuizType.MEANING_MATCH, 30)));
        when(questionRepository.findAllByTypeAndActiveTrue(QuizType.FILL_LETTERS))
                .thenReturn(List.of(question("fill-1", "GRY", QuizType.FILL_LETTERS, 30)));
    }

    private static QuizQuestion question(String id, String answer, QuizType type, int points) {
        return QuizQuestion.builder()
                .id(id).code(id).type(type).prompt("Prompt").answer(answer)
                .maximumPoints(points).active(true).build();
    }

    private static QuizSession activeSession() {
        Instant now = Instant.now();
        return QuizSession.builder()
                .id("session-1")
                .userId("user-1")
                .userName("Amaya Perera")
                .status(QuizSessionStatus.IN_PROGRESS)
                .currentStage(0)
                .startedAt(now)
                .stages(List.of(
                        QuizStageAttempt.builder().questionId("builder-1").type(QuizType.WORD_BUILDER)
                                .prompt("Prompt").answer("STUPA").letters(List.of("S", "T", "U", "P", "A"))
                                .maximumPoints(40).issuedAt(now).deadlineAt(now.plusSeconds(30)).build(),
                        QuizStageAttempt.builder().questionId("meaning-1").type(QuizType.MEANING_MATCH)
                                .prompt("Meaning").answer("Correct").options(List.of("Correct", "Wrong"))
                                .maximumPoints(30).build(),
                        QuizStageAttempt.builder().questionId("fill-1").type(QuizType.FILL_LETTERS)
                                .prompt("Fill").answer("GRY").displayWord("SI_I_I_A")
                                .letters(List.of("G", "R", "Y")).blankPositions(List.of(2, 4, 6))
                                .maximumPoints(30).build()))
                .build();
    }
}
