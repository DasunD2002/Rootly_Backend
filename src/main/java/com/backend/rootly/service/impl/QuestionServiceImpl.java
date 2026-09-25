package com.backend.rootly.service.impl;

import com.backend.rootly.domain.CreateQuestionCommentDomain;
import com.backend.rootly.domain.CreateQuestionDomain;
import com.backend.rootly.domain.ForumVoteDomain;
import com.backend.rootly.domain.QuestionSearchDomain;
import com.backend.rootly.dto.response.ForumAuthorDTO;
import com.backend.rootly.dto.response.ForumVoteResponseDTO;
import com.backend.rootly.dto.response.QuestionBookmarkResponseDTO;
import com.backend.rootly.dto.response.QuestionCommentResponseDTO;
import com.backend.rootly.dto.response.QuestionDetailDTO;
import com.backend.rootly.dto.response.QuestionPageResponseDTO;
import com.backend.rootly.dto.response.QuestionSummaryDTO;
import com.backend.rootly.entity.ForumVote;
import com.backend.rootly.entity.Question;
import com.backend.rootly.entity.QuestionBookmark;
import com.backend.rootly.entity.QuestionComment;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CommentStatus;
import com.backend.rootly.enums.ForumTargetType;
import com.backend.rootly.enums.QuestionCategory;
import com.backend.rootly.enums.QuestionStatus;
import com.backend.rootly.exception.ResourceNotFoundException;
import com.backend.rootly.repository.ForumVoteRepository;
import com.backend.rootly.repository.QuestionBookmarkRepository;
import com.backend.rootly.repository.QuestionCommentRepository;
import com.backend.rootly.repository.QuestionRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.QuestionService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntConsumer;

@Service
@Log4j2
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveParameterList", "PMD.CouplingBetweenObjects"})
public class QuestionServiceImpl implements QuestionService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 120;
    private static final int MAX_COMMENT_DEPTH = 8;
    private static final String SORT_TOP = "top";
    private static final String SORT_NEW = "new";
    private static final String SORT_UNANSWERED = "unanswered";

    private final QuestionRepository questionRepository;
    private final QuestionCommentRepository commentRepository;
    private final ForumVoteRepository voteRepository;
    private final QuestionBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final ResponseGenerator responseGenerator;
    private final Clock clock;

    @Autowired
    public QuestionServiceImpl(QuestionRepository questionRepository,
                               QuestionCommentRepository commentRepository,
                               ForumVoteRepository voteRepository,
                               QuestionBookmarkRepository bookmarkRepository,
                               UserRepository userRepository,
                               ResponseGenerator responseGenerator,
                               @Autowired(required = false) Clock clock) {
        this.questionRepository = questionRepository;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.responseGenerator = responseGenerator;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public ResponseEntity<Object> getQuestions(QuestionSearchDomain request, UserReg viewer, Locale locale) {
        validateSearch(request);
        String normalizedSort = normalizeSort(request.getSort(), true);
        Sort sort = SORT_TOP.equals(normalizedSort)
                ? Sort.by(Sort.Order.desc("voteScore"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        PageRequest pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Page<Question> result = questionRepository.search(
                trimToNull(request.getQuery()),
                request.getCategory(),
                SORT_UNANSWERED.equals(normalizedSort),
                pageable
        );

        List<Question> questions = result.getContent();
        Set<String> questionIds = collectQuestionIds(questions);
        Map<String, UserReg> authors = loadAuthors(questions.stream().map(Question::getAuthorId).toList());
        Map<String, Integer> viewerVotes = loadViewerVotes(viewer, ForumTargetType.QUESTION, questionIds);
        Set<String> bookmarkedQuestionIds = loadBookmarks(viewer, questionIds);
        List<QuestionSummaryDTO> items = questions.stream()
                .map(question -> toSummary(question, authors.get(question.getAuthorId()), viewer,
                        viewerVotes.getOrDefault(question.getId(), 0),
                        bookmarkedQuestionIds.contains(question.getId())))
                .toList();

        QuestionPageResponseDTO response = QuestionPageResponseDTO.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .total(result.getTotalElements())
                .hasNext(result.hasNext())
                .build();
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.QUESTION_LIST_SUCCESS, MessageConstant.QUESTION_LIST_SUCCESS, locale, response);
    }

    @Override
    public ResponseEntity<Object> getQuestion(String questionId, String commentSort,
                                              UserReg viewer, Locale locale) {
        Question question = requireQuestion(questionId);
        String normalizedSort = normalizeSort(commentSort, false);
        List<QuestionComment> comments = commentRepository
                .findByQuestionIdAndStatusOrderByCreatedAtAsc(question.getId(), CommentStatus.ACTIVE);

        Set<String> authorIds = new HashSet<>();
        authorIds.add(question.getAuthorId());
        comments.stream().map(QuestionComment::getAuthorId).forEach(authorIds::add);
        Map<String, UserReg> authors = loadAuthors(authorIds);
        Set<String> commentIds = collectCommentIds(comments);
        Map<String, Integer> commentVotes = loadViewerVotes(viewer, ForumTargetType.COMMENT, commentIds);
        List<QuestionCommentResponseDTO> commentTree = buildCommentTree(
                comments, authors, viewer, commentVotes, normalizedSort);

        int viewerVote = loadViewerVote(viewer, ForumTargetType.QUESTION, question.getId());
        boolean bookmarked = viewer != null && bookmarkRepository
                .findByUserIdAndQuestionId(requireUserId(viewer), question.getId()).isPresent();
        QuestionDetailDTO response = toDetail(question, authors.get(question.getAuthorId()), viewer,
                viewerVote, bookmarked, commentTree);
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.QUESTION_DETAIL_SUCCESS, MessageConstant.QUESTION_DETAIL_SUCCESS, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createQuestion(CreateQuestionDomain request, UserReg author, Locale locale) {
        String authorId = requireUserId(author);
        String title = requireText(request == null ? null : request.getTitle(), 10, 200, "title");
        String body = requireText(request.getBody(), 20, 5_000, "body");
        String location = requireText(request.getLocation(), 1, 120, "location");
        QuestionCategory category = request.getCategory();
        if (category == null) {
            throw new IllegalArgumentException("category is required");
        }

        Instant now = Instant.now(clock);
        Question question = Question.builder()
                .title(title)
                .body(body)
                .location(location)
                .placeId(trimToNull(request.getPlaceId()))
                .category(category)
                .authorId(authorId)
                .voteScore(0)
                .commentCount(0)
                .verified(false)
                .status(QuestionStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Question saved = questionRepository.save(question);
        QuestionDetailDTO response = toDetail(saved, author, author, 0, false, List.of());

        log.info("Question {} created by user {}", saved.getId(), authorId);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.QUESTION_CREATE_SUCCESS, MessageConstant.QUESTION_CREATE_SUCCESS, locale, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> createComment(String questionId, CreateQuestionCommentDomain request,
                                                UserReg author, Locale locale) {
        String authorId = requireUserId(author);
        Question question = requireQuestion(questionId);
        if (question.getStatus() != QuestionStatus.OPEN) {
            throw new IllegalStateException("Comments cannot be added to a closed question");
        }
        String body = requireText(request == null ? null : request.getBody(), 1, 5_000, "body");
        String parentId = trimToNull(request == null ? null : request.getParentCommentId());
        String commentId = UUID.randomUUID().toString();
        String rootId = commentId;
        if (parentId != null) {
            QuestionComment parent = requireComment(parentId);
            if (!question.getId().equals(parent.getQuestionId())) {
                throw new IllegalArgumentException("parentCommentId does not belong to this question");
            }
            validateReplyDepth(parent);
            rootId = parent.getRootCommentId();
        }

        Instant now = Instant.now(clock);
        QuestionComment comment = QuestionComment.builder()
                .id(commentId)
                .questionId(question.getId())
                .parentCommentId(parentId)
                .rootCommentId(rootId)
                .authorId(authorId)
                .body(body)
                .voteScore(0)
                .verified(false)
                .accepted(false)
                .status(CommentStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        QuestionComment saved = commentRepository.save(comment);
        if (parentId == null) {
            question.setCommentCount(question.getCommentCount() + 1);
            question.setUpdatedAt(now);
            questionRepository.save(question);
        }

        QuestionCommentResponseDTO response = toCommentResponse(saved, author, author, 0);
        log.info("Comment {} created on question {} by user {}", saved.getId(), question.getId(), authorId);
        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.QUESTION_COMMENT_CREATE_SUCCESS,
                MessageConstant.QUESTION_COMMENT_CREATE_SUCCESS, locale, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> voteQuestion(String questionId, ForumVoteDomain request,
                                               UserReg voter, Locale locale) {
        String voterId = requireUserId(voter);
        Question question = requireQuestion(questionId);
        ForumVoteResponseDTO response = applyVote(voterId, ForumTargetType.QUESTION,
                question.getId(), requireVoteValue(request), question.getVoteScore(), score -> {
                    question.setVoteScore(score);
                    question.setUpdatedAt(Instant.now(clock));
                    questionRepository.save(question);
                });
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.QUESTION_VOTE_SUCCESS, MessageConstant.QUESTION_VOTE_SUCCESS, locale, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> voteComment(String commentId, ForumVoteDomain request,
                                              UserReg voter, Locale locale) {
        String voterId = requireUserId(voter);
        QuestionComment comment = requireComment(commentId);
        ForumVoteResponseDTO response = applyVote(voterId, ForumTargetType.COMMENT,
                comment.getId(), requireVoteValue(request), comment.getVoteScore(), score -> {
                    comment.setVoteScore(score);
                    comment.setUpdatedAt(Instant.now(clock));
                    commentRepository.save(comment);
                });
        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.QUESTION_VOTE_SUCCESS, MessageConstant.QUESTION_VOTE_SUCCESS, locale, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> bookmarkQuestion(String questionId, UserReg user, Locale locale) {
        String userId = requireUserId(user);
        Question question = requireQuestion(questionId);
        bookmarkRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> bookmarkRepository.save(QuestionBookmark.builder()
                        .userId(userId)
                        .questionId(question.getId())
                        .createdAt(Instant.now(clock))
                        .build()));
        QuestionBookmarkResponseDTO response = QuestionBookmarkResponseDTO.builder()
                .questionId(question.getId())
                .bookmarked(true)
                .build();
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.QUESTION_BOOKMARK_SUCCESS, MessageConstant.QUESTION_BOOKMARK_SUCCESS, response);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> removeQuestionBookmark(String questionId, UserReg user, Locale locale) {
        String userId = requireUserId(user);
        Question question = requireQuestion(questionId);
        bookmarkRepository.findByUserIdAndQuestionId(userId, question.getId())
                .ifPresent(bookmarkRepository::delete);
        QuestionBookmarkResponseDTO response = QuestionBookmarkResponseDTO.builder()
                .questionId(question.getId())
                .bookmarked(false)
                .build();
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.QUESTION_BOOKMARK_SUCCESS, MessageConstant.QUESTION_BOOKMARK_REMOVE_SUCCESS, response);
    }

    private ForumVoteResponseDTO applyVote(String userId, ForumTargetType targetType, String targetId,
                                           int requestedValue, int currentScore, IntConsumer scoreUpdater) {
        ForumVote existing = voteRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, targetType, targetId).orElse(null);
        int previousValue = existing == null ? 0 : existing.getValue();
        Instant now = Instant.now(clock);

        if (requestedValue == 0) {
            if (existing != null) {
                voteRepository.delete(existing);
            }
        } else if (existing == null) {
            voteRepository.save(ForumVote.builder()
                    .userId(userId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .value(requestedValue)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        } else {
            existing.setValue(requestedValue);
            existing.setUpdatedAt(now);
            voteRepository.save(existing);
        }

        int updatedScore = currentScore + requestedValue - previousValue;
        scoreUpdater.accept(updatedScore);
        return ForumVoteResponseDTO.builder()
                .targetId(targetId)
                .voteScore(updatedScore)
                .viewerVote(requestedValue)
                .build();
    }

    private List<QuestionCommentResponseDTO> buildCommentTree(
            List<QuestionComment> comments,
            Map<String, UserReg> authors,
            UserReg viewer,
            Map<String, Integer> viewerVotes,
            String commentSort) {
        Map<String, QuestionCommentResponseDTO> responses = new LinkedHashMap<>();
        for (QuestionComment comment : comments) {
            responses.put(comment.getId(), toCommentResponse(comment, authors.get(comment.getAuthorId()),
                    viewer, viewerVotes.getOrDefault(comment.getId(), 0)));
        }

        List<QuestionCommentResponseDTO> roots = new ArrayList<>();
        for (QuestionComment comment : comments) {
            QuestionCommentResponseDTO response = responses.get(comment.getId());
            QuestionCommentResponseDTO parent = responses.get(comment.getParentCommentId());
            if (comment.getParentCommentId() == null || parent == null) {
                roots.add(response);
            } else {
                parent.getReplies().add(response);
            }
        }

        Comparator<QuestionCommentResponseDTO> comparator = SORT_TOP.equals(commentSort)
                ? Comparator.comparingInt(QuestionCommentResponseDTO::getVoteScore).reversed()
                        .thenComparing(QuestionCommentResponseDTO::getCreatedAt)
                : Comparator.comparing(QuestionCommentResponseDTO::getCreatedAt).reversed();
        roots.sort(comparator);
        return roots;
    }

    private QuestionSummaryDTO toSummary(Question question, UserReg author, UserReg viewer,
                                         int viewerVote, boolean bookmarked) {
        QuestionCategory category = question.getCategory();
        return QuestionSummaryDTO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .body(question.getBody())
                .location(question.getLocation())
                .placeId(question.getPlaceId())
                .category(category == null ? null : category.getDisplayName())
                .categoryId(category == null ? null : category.getSlug())
                .author(toAuthor(author, question.getAuthorId()))
                .voteScore(question.getVoteScore())
                .commentCount(question.getCommentCount())
                .verified(question.isVerified())
                .viewerVote(viewerVote)
                .bookmarked(bookmarked)
                .ownedByViewer(isOwner(viewer, question.getAuthorId()))
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private QuestionDetailDTO toDetail(Question question, UserReg author, UserReg viewer,
                                       int viewerVote, boolean bookmarked,
                                       List<QuestionCommentResponseDTO> comments) {
        QuestionCategory category = question.getCategory();
        return QuestionDetailDTO.builder()
                .id(question.getId())
                .title(question.getTitle())
                .body(question.getBody())
                .location(question.getLocation())
                .placeId(question.getPlaceId())
                .category(category == null ? null : category.getDisplayName())
                .categoryId(category == null ? null : category.getSlug())
                .author(toAuthor(author, question.getAuthorId()))
                .voteScore(question.getVoteScore())
                .commentCount(question.getCommentCount())
                .acceptedCommentId(question.getAcceptedCommentId())
                .verified(question.isVerified())
                .viewerVote(viewerVote)
                .bookmarked(bookmarked)
                .ownedByViewer(isOwner(viewer, question.getAuthorId()))
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .comments(comments)
                .build();
    }

    private QuestionCommentResponseDTO toCommentResponse(QuestionComment comment, UserReg author,
                                                         UserReg viewer, int viewerVote) {
        return QuestionCommentResponseDTO.builder()
                .id(comment.getId())
                .questionId(comment.getQuestionId())
                .parentCommentId(comment.getParentCommentId())
                .author(toAuthor(author, comment.getAuthorId()))
                .body(comment.getBody())
                .voteScore(comment.getVoteScore())
                .viewerVote(viewerVote)
                .verified(comment.isVerified())
                .accepted(comment.isAccepted())
                .ownedByViewer(isOwner(viewer, comment.getAuthorId()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(new ArrayList<>())
                .build();
    }

    private static ForumAuthorDTO toAuthor(UserReg user, String fallbackId) {
        if (user == null) {
            return ForumAuthorDTO.builder()
                    .id(fallbackId)
                    .name("Unknown user")
                    .initials("?")
                    .role("")
                    .verified(false)
                    .build();
        }
        return ForumAuthorDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .initials(initials(user.getName()))
                .role(formatRole(user.getRole()))
                .photoUrl(user.getPhotoUrl())
                .verified("VERIFIED".equalsIgnoreCase(user.getVerificationStatus()))
                .build();
    }

    private Map<String, UserReg> loadAuthors(Collection<String> authorIds) {
        Map<String, UserReg> authors = new HashMap<>();
        Set<String> validIds = new HashSet<>();
        authorIds.stream().filter(id -> id != null && !id.isBlank()).forEach(validIds::add);
        if (!validIds.isEmpty()) {
            userRepository.findAllById(validIds).forEach(user -> authors.put(user.getId(), user));
        }
        return authors;
    }

    private Map<String, Integer> loadViewerVotes(UserReg viewer, ForumTargetType targetType,
                                                 Collection<String> targetIds) {
        Map<String, Integer> votes = new HashMap<>();
        if (viewer == null || targetIds.isEmpty()) {
            return votes;
        }
        voteRepository.findByUserIdAndTargetTypeAndTargetIdIn(
                        requireUserId(viewer), targetType, targetIds)
                .forEach(vote -> votes.put(vote.getTargetId(), vote.getValue()));
        return votes;
    }

    private int loadViewerVote(UserReg viewer, ForumTargetType targetType, String targetId) {
        if (viewer == null) {
            return 0;
        }
        return voteRepository.findByUserIdAndTargetTypeAndTargetId(
                        requireUserId(viewer), targetType, targetId)
                .map(ForumVote::getValue)
                .orElse(0);
    }

    private Set<String> loadBookmarks(UserReg viewer, Collection<String> questionIds) {
        Set<String> bookmarkedIds = new HashSet<>();
        if (viewer == null || questionIds.isEmpty()) {
            return bookmarkedIds;
        }
        bookmarkRepository.findByUserIdAndQuestionIdIn(requireUserId(viewer), questionIds)
                .forEach(bookmark -> bookmarkedIds.add(bookmark.getQuestionId()));
        return bookmarkedIds;
    }

    private Question requireQuestion(String questionId) {
        String normalizedId = requireId(questionId, "questionId");
        Question question = questionRepository.findById(normalizedId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        if (question.getStatus() == QuestionStatus.DELETED) {
            throw new ResourceNotFoundException("Question not found");
        }
        return question;
    }

    private QuestionComment requireComment(String commentId) {
        String normalizedId = requireId(commentId, "commentId");
        return commentRepository.findByIdAndStatus(normalizedId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    private void validateReplyDepth(QuestionComment parent) {
        int depth = 1;
        String ancestorId = parent.getParentCommentId();
        while (ancestorId != null) {
            QuestionComment ancestor = requireComment(ancestorId);
            ancestorId = ancestor.getParentCommentId();
            depth++;
        }
        if (depth >= MAX_COMMENT_DEPTH) {
            throw new IllegalArgumentException("Maximum comment reply depth exceeded");
        }
    }

    private static void validateSearch(QuestionSearchDomain request) {
        if (request == null) {
            throw new IllegalArgumentException("Question search request is required");
        }
        if (request.getPage() < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (request.getSize() < 1 || request.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and 50");
        }
        String query = trimToNull(request.getQuery());
        if (query != null && query.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("q must not exceed 120 characters");
        }
    }

    private static String normalizeSort(String sort, boolean allowUnanswered) {
        String normalized = sort == null ? SORT_TOP : sort.trim().toLowerCase(Locale.ROOT);
        if (SORT_TOP.equals(normalized) || SORT_NEW.equals(normalized)
                || allowUnanswered && SORT_UNANSWERED.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException(allowUnanswered
                ? "sort must be top, new, or unanswered"
                : "commentSort must be top or new");
    }

    private static int requireVoteValue(ForumVoteDomain request) {
        int value = request == null ? Integer.MIN_VALUE : request.getValue();
        if (value < -1 || value > 1) {
            throw new IllegalArgumentException("value must be -1, 0, or 1");
        }
        return value;
    }

    private static String requireUserId(UserReg user) {
        if (user == null) {
            throw new IllegalStateException("Authenticated user is required");
        }
        return requireId(user.getId(), "Authenticated user id");
    }

    private static String requireId(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return normalized;
    }

    private static String requireText(String value, int minimum, int maximum, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() < minimum || normalized.length() > maximum) {
            throw new IllegalArgumentException(fieldName + " must contain between "
                    + minimum + " and " + maximum + " characters");
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Set<String> collectQuestionIds(List<Question> questions) {
        Set<String> ids = new HashSet<>();
        questions.forEach(question -> ids.add(question.getId()));
        return ids;
    }

    private static Set<String> collectCommentIds(List<QuestionComment> comments) {
        Set<String> ids = new HashSet<>();
        comments.forEach(comment -> ids.add(comment.getId()));
        return ids;
    }

    private static boolean isOwner(UserReg viewer, String authorId) {
        return viewer != null && viewer.getId() != null && viewer.getId().equals(authorId);
    }

    private static String initials(String name) {
        String normalized = trimToNull(name);
        if (normalized == null) {
            return "?";
        }
        StringBuilder result = new StringBuilder(2);
        for (String part : normalized.split("\\s+")) {
            if (!part.isEmpty() && result.length() < 2) {
                result.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return result.toString();
    }

    private static String formatRole(String role) {
        String normalized = trimToNull(role);
        if (normalized == null || "USER".equalsIgnoreCase(normalized)) {
            return "Community member";
        }
        String lowerCase = normalized.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
