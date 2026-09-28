package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.SubmitQuizAnswerRequestDTO;
import com.backend.rootly.dto.response.QuizAnswerResponseDTO;
import com.backend.rootly.dto.response.QuizDashboardResponseDTO;
import com.backend.rootly.dto.response.QuizLeaderResponseDTO;
import com.backend.rootly.dto.response.QuizQuestionResponseDTO;
import com.backend.rootly.dto.response.QuizSessionResponseDTO;
import com.backend.rootly.entity.QuizQuestion;
import com.backend.rootly.entity.QuizSession;
import com.backend.rootly.entity.QuizStageAttempt;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.QuizSessionStatus;
import com.backend.rootly.enums.QuizType;
import com.backend.rootly.exception.ResourceNotFoundException;
import com.backend.rootly.repository.QuizQuestionRepository;
import com.backend.rootly.repository.QuizSessionRepository;
import com.backend.rootly.service.QuizService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@SuppressWarnings({
        "PMD.TooManyMethods",
        "PMD.GodClass",
        "PMD.ExcessiveImports",
        "PMD.CouplingBetweenObjects",
        "PMD.CyclomaticComplexity",
        "PMD.LawOfDemeter"
})
public class QuizServiceImpl implements QuizService {

    static final int TIME_LIMIT_SECONDS = 30;
    private static final ZoneId QUIZ_ZONE = ZoneId.of("Asia/Colombo");
    private static final List<QuizType> STAGE_ORDER = List.of(
            QuizType.WORD_BUILDER, QuizType.MEANING_MATCH, QuizType.FILL_LETTERS);

    private final QuizQuestionRepository questionRepository;
    private final QuizSessionRepository sessionRepository;
    private final ResponseGenerator responseGenerator;

    @Override
    @Transactional
    public ResponseEntity<Object> dashboard(UserReg user, Locale locale) {
        requireUser(user);
        Instant now = Instant.now();
        Optional<QuizSession> active = sessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(user.getId(), QuizSessionStatus.IN_PROGRESS)
                .map(session -> expireCurrentStage(session, now))
                .filter(session -> session.getStatus() == QuizSessionStatus.IN_PROGRESS);
        List<QuizSession> userSessions = sessionRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        LocalDate today = LocalDate.ofInstant(now, QUIZ_ZONE);
        int todayScore = bestScoreForDate(userSessions, today);
        boolean completedToday = completedDays(userSessions).contains(today);
        int completedSteps = active.map(QuizSession::getCurrentStage)
                .orElse(completedToday ? STAGE_ORDER.size() : 0);
        int visibleScore = active.map(QuizSession::getTotalScore).orElse(todayScore);

        QuizDashboardResponseDTO result = QuizDashboardResponseDTO.builder()
                .completedSteps(completedSteps)
                .todayScore(visibleScore)
                .streakDays(calculateStreak(userSessions, today))
                .activeSessionId(active.map(QuizSession::getId).orElse(null))
                .weekActivity(weekActivity(userSessions, today))
                .weeklyLeaders(weeklyLeaders(user, now))
                .build();
        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.QUIZ_DASHBOARD_SUCCESS, MessageConstant.QUIZ_DASHBOARD_SUCCESS, locale, result);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> startOrResume(UserReg user, Locale locale) {
        requireUser(user);
        Instant now = Instant.now();
        QuizSession session = sessionRepository
                .findFirstByUserIdAndStatusOrderByStartedAtDesc(user.getId(), QuizSessionStatus.IN_PROGRESS)
                .map(value -> expireCurrentStage(value, now))
                .filter(value -> value.getStatus() == QuizSessionStatus.IN_PROGRESS)
                .orElseGet(() -> createSession(user, now));
        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.QUIZ_SESSION_SUCCESS, MessageConstant.QUIZ_SESSION_SUCCESS,
                locale, toSessionResponse(session));
    }

    @Override
    @Transactional
    public ResponseEntity<Object> submitAnswer(String sessionId, SubmitQuizAnswerRequestDTO request,
                                               UserReg user, Locale locale) {
        requireUser(user);
        if (request == null || request.getQuestionId() == null || request.getQuestionId().isBlank()) {
            throw new IllegalArgumentException("questionId is required");
        }
        QuizSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz session was not found"));
        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("This quiz session is already complete");
        }
        QuizStageAttempt stage = currentStage(session);
        if (!stage.getQuestionId().equals(request.getQuestionId())) {
            throw new IllegalArgumentException("The submitted question is not the current quiz stage");
        }

        Instant now = Instant.now();
        if (request.isTimedOut() || !now.isBefore(stage.getDeadlineAt())) {
            completeStage(session, stage, false, true, 0, now);
            QuizAnswerResponseDTO result = answerResult(session, false, true, 0,
                    "Time is up. Moving to the next challenge.");
            return quizAnswerResponse(locale, result);
        }
        if (request.getAnswer() == null || request.getAnswer().isBlank()) {
            throw new IllegalArgumentException("answer is required while time remains");
        }

        stage.setAttemptCount(stage.getAttemptCount() + 1);
        boolean correct = normalize(request.getAnswer()).equals(normalize(stage.getAnswer()));
        if (!correct) {
            sessionRepository.save(session);
            QuizAnswerResponseDTO result = answerResult(session, false, false, 0,
                    "Not quite. Try again while time remains.");
            return quizAnswerResponse(locale, result);
        }

        int points = calculatePoints(stage, now);
        completeStage(session, stage, true, false, points, now);
        QuizAnswerResponseDTO result = answerResult(session, true, false, points,
                session.getStatus() == QuizSessionStatus.COMPLETED
                        ? "Daily challenge complete!" : "Correct!");
        return quizAnswerResponse(locale, result);
    }

    private ResponseEntity<Object> quizAnswerResponse(Locale locale, QuizAnswerResponseDTO result) {
        return responseGenerator.generateSuccessResponse(null, HttpStatus.OK,
                ResponseCode.QUIZ_ANSWER_SUCCESS, MessageConstant.QUIZ_ANSWER_SUCCESS, locale, result);
    }

    private QuizSession createSession(UserReg user, Instant now) {
        List<QuizSession> history = sessionRepository.findByUserIdOrderByStartedAtDesc(user.getId());
        List<QuizStageAttempt> stages = new ArrayList<>();
        for (QuizType type : STAGE_ORDER) {
            stages.add(toAttempt(selectQuestion(type, history)));
        }
        issue(stages.get(0), now);
        QuizSession session = QuizSession.builder()
                .userId(user.getId())
                .userName(user.getName() == null || user.getName().isBlank() ? "Rootly learner" : user.getName())
                .status(QuizSessionStatus.IN_PROGRESS)
                .currentStage(0)
                .totalScore(0)
                .startedAt(now)
                .stages(stages)
                .build();
        return sessionRepository.save(session);
    }

    private QuizQuestion selectQuestion(QuizType type, List<QuizSession> history) {
        List<QuizQuestion> pool = new ArrayList<>(questionRepository.findAllByTypeAndActiveTrue(type));
        if (pool.isEmpty()) {
            throw new IllegalStateException("No active quiz questions are available for " + type);
        }
        Set<String> used = new HashSet<>();
        String mostRecent = null;
        for (QuizSession previous : history) {
            for (QuizStageAttempt stage : previous.getStages()) {
                if (stage.getType() == type) {
                    if (mostRecent == null) {
                        mostRecent = stage.getQuestionId();
                    }
                    used.add(stage.getQuestionId());
                }
            }
        }
        List<QuizQuestion> candidates = pool.stream()
                .filter(question -> !used.contains(question.getId()))
                .toList();
        if (candidates.isEmpty()) {
            String excluded = mostRecent;
            candidates = pool.stream()
                    .filter(question -> pool.size() == 1 || !question.getId().equals(excluded))
                    .toList();
        }
        List<QuizQuestion> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        return shuffled.get(0);
    }

    private static QuizStageAttempt toAttempt(QuizQuestion question) {
        List<String> letters = question.getLetters() == null || question.getLetters().isEmpty()
                ? question.getAnswer().codePoints().mapToObj(value -> new String(Character.toChars(value))).toList()
                : question.getLetters();
        List<String> shuffledLetters = new ArrayList<>(letters);
        List<String> shuffledOptions = new ArrayList<>(question.getOptions() == null
                ? List.of() : question.getOptions());
        Collections.shuffle(shuffledLetters);
        Collections.shuffle(shuffledOptions);
        return QuizStageAttempt.builder()
                .questionId(question.getId())
                .type(question.getType())
                .prompt(question.getPrompt())
                .answer(question.getAnswer())
                .displayWord(question.getDisplayWord())
                .letters(shuffledLetters)
                .options(shuffledOptions)
                .blankPositions(question.getBlankPositions() == null
                        ? List.of() : List.copyOf(question.getBlankPositions()))
                .maximumPoints(question.getMaximumPoints())
                .build();
    }

    private QuizSession expireCurrentStage(QuizSession session, Instant now) {
        if (session.getStatus() != QuizSessionStatus.IN_PROGRESS) {
            return session;
        }
        QuizStageAttempt stage = currentStage(session);
        if (stage.getDeadlineAt() != null && !now.isBefore(stage.getDeadlineAt())) {
            completeStage(session, stage, false, true, 0, now);
        }
        return session;
    }

    private void completeStage(QuizSession session, QuizStageAttempt stage, boolean correct,
                               boolean timedOut, int points, Instant now) {
        stage.setCorrect(correct);
        stage.setTimedOut(timedOut);
        stage.setPointsAwarded(points);
        stage.setCompletedAt(now);
        session.setTotalScore(Math.min(100, session.getTotalScore() + points));
        int nextStage = session.getCurrentStage() + 1;
        if (nextStage >= session.getStages().size()) {
            session.setCurrentStage(session.getStages().size());
            session.setStatus(QuizSessionStatus.COMPLETED);
            session.setCompletedAt(now);
        } else {
            session.setCurrentStage(nextStage);
            issue(session.getStages().get(nextStage), now);
        }
        sessionRepository.save(session);
    }

    private static void issue(QuizStageAttempt stage, Instant now) {
        stage.setIssuedAt(now);
        stage.setDeadlineAt(now.plusSeconds(TIME_LIMIT_SECONDS));
    }

    private static int calculatePoints(QuizStageAttempt stage, Instant now) {
        long remaining = Math.max(0, Duration.between(now, stage.getDeadlineAt()).toSeconds());
        double speedRatio = Math.min(1.0, remaining / (double) TIME_LIMIT_SECONDS);
        double baseWithSpeed = stage.getMaximumPoints() * (0.70 + 0.30 * speedRatio);
        double wrongAttemptPenalty = stage.getMaximumPoints() * 0.05
                * Math.max(0, stage.getAttemptCount() - 1);
        return Math.max(0, (int) Math.round(baseWithSpeed - wrongAttemptPenalty));
    }

    private static QuizStageAttempt currentStage(QuizSession session) {
        if (session.getCurrentStage() < 0 || session.getCurrentStage() >= session.getStages().size()) {
            throw new IllegalStateException("Quiz session has no active stage");
        }
        return session.getStages().get(session.getCurrentStage());
    }

    private static QuizAnswerResponseDTO answerResult(QuizSession session, boolean correct,
                                                      boolean timedOut, int points, String feedback) {
        return QuizAnswerResponseDTO.builder()
                .correct(correct)
                .timedOut(timedOut)
                .completed(session.getStatus() == QuizSessionStatus.COMPLETED)
                .feedback(feedback)
                .pointsAwarded(points)
                .totalScore(session.getTotalScore())
                .completedSteps(session.getCurrentStage())
                .session(toSessionResponse(session))
                .build();
    }

    private static QuizSessionResponseDTO toSessionResponse(QuizSession session) {
        QuizQuestionResponseDTO question = null;
        if (session.getStatus() == QuizSessionStatus.IN_PROGRESS) {
            question = toQuestionResponse(currentStage(session));
        }
        return QuizSessionResponseDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .currentStage(session.getCurrentStage())
                .completedSteps(session.getCurrentStage())
                .totalScore(session.getTotalScore())
                .question(question)
                .build();
    }

    private static QuizQuestionResponseDTO toQuestionResponse(QuizStageAttempt stage) {
        return QuizQuestionResponseDTO.builder()
                .id(stage.getQuestionId())
                .type(stage.getType())
                .prompt(stage.getPrompt())
                .displayWord(stage.getDisplayWord())
                .letters(List.copyOf(stage.getLetters()))
                .options(List.copyOf(stage.getOptions()))
                .blankPositions(List.copyOf(stage.getBlankPositions()))
                .targetLength(stage.getType() == QuizType.WORD_BUILDER
                        ? stage.getAnswer().length() : 0)
                .maximumPoints(stage.getMaximumPoints())
                .timeLimitSeconds(TIME_LIMIT_SECONDS)
                .deadlineAt(stage.getDeadlineAt())
                .build();
    }

    private List<QuizLeaderResponseDTO> weeklyLeaders(UserReg viewer, Instant now) {
        ZonedDateTime current = now.atZone(QUIZ_ZONE);
        Instant weekStart = current.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(QUIZ_ZONE).toInstant();
        Instant weekEnd = weekStart.plus(Duration.ofDays(7));
        List<QuizSession> completed = sessionRepository.findByStatusAndCompletedAtBetween(
                QuizSessionStatus.COMPLETED, weekStart, weekEnd);

        Map<String, Map<LocalDate, Integer>> dailyBest = new HashMap<>();
        Map<String, String> names = new HashMap<>();
        for (QuizSession session : completed) {
            LocalDate date = LocalDate.ofInstant(session.getCompletedAt(), QUIZ_ZONE);
            dailyBest.computeIfAbsent(session.getUserId(), ignored -> new HashMap<>())
                    .merge(date, session.getTotalScore(), Math::max);
            names.put(session.getUserId(), session.getUserName());
        }
        Map<String, Integer> totals = new LinkedHashMap<>();
        dailyBest.forEach((userId, scores) -> totals.put(userId,
                scores.values().stream().mapToInt(Integer::intValue).sum()));
        List<Map.Entry<String, Integer>> ranked = totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(3)
                .toList();
        List<QuizLeaderResponseDTO> leaders = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            Map.Entry<String, Integer> entry = ranked.get(index);
            String name = names.getOrDefault(entry.getKey(), "Rootly learner");
            leaders.add(QuizLeaderResponseDTO.builder()
                    .rank(index + 1)
                    .userId(entry.getKey())
                    .name(name)
                    .initials(initials(name))
                    .score(entry.getValue())
                    .viewer(entry.getKey().equals(viewer.getId()))
                    .build());
        }
        return leaders;
    }

    private static int calculateStreak(List<QuizSession> sessions, LocalDate today) {
        Set<LocalDate> completedDays = completedDays(sessions);
        LocalDate cursor = completedDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (completedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static List<Boolean> weekActivity(List<QuizSession> sessions, LocalDate today) {
        Set<LocalDate> completedDays = completedDays(sessions);
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<Boolean> activity = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            activity.add(completedDays.contains(monday.plusDays(day)));
        }
        return activity;
    }

    private static Set<LocalDate> completedDays(List<QuizSession> sessions) {
        Set<LocalDate> days = new HashSet<>();
        sessions.stream()
                .filter(session -> session.getStatus() == QuizSessionStatus.COMPLETED)
                .filter(session -> session.getCompletedAt() != null)
                .forEach(session -> days.add(LocalDate.ofInstant(session.getCompletedAt(), QUIZ_ZONE)));
        return days;
    }

    private static int bestScoreForDate(List<QuizSession> sessions, LocalDate date) {
        return sessions.stream()
                .filter(session -> session.getStatus() == QuizSessionStatus.COMPLETED)
                .filter(session -> session.getCompletedAt() != null)
                .filter(session -> LocalDate.ofInstant(session.getCompletedAt(), QUIZ_ZONE).equals(date))
                .mapToInt(QuizSession::getTotalScore)
                .max().orElse(0);
    }

    private static String initials(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder value = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty() && value.length() < 2) {
                value.append(Character.toUpperCase(word.charAt(0)));
            }
        }
        return value.isEmpty() ? "RL" : value.toString();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    private static void requireUser(UserReg user) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw new IllegalArgumentException("An authenticated user is required");
        }
    }
}
