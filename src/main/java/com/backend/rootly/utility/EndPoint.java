package com.backend.rootly.utility;

@SuppressWarnings("PMD.DataClass")
public final class EndPoint {

    private EndPoint() {
    }

    public static final String API = "/api";

    // Auth Endpoints
    public static final String AUTH_REGISTER = "/v1/auth/register";
    public static final String AUTH_LOGIN = "/v1/auth/login";

    // Capsule Endpoints
    public static final String CAPSULES = "/v1/capsules";
    public static final String CAPSULE_DETAIL = "/v1/capsules/{capsuleId}";
    public static final String CAPSULE_INVITE = "/v1/capsules/{capsuleId}/invite";
    public static final String CAPSULE_CONTRIBUTORS = "/v1/capsules/{capsuleId}/contributors";

    // User Endpoints
    public static final String USER_PROFILE = "/v1/users/{userId}";

    // Question Forum Endpoints
    public static final String QUESTIONS = "/v1/questions";
    public static final String QUESTION_DETAIL = "/v1/questions/{questionId}";
    public static final String QUESTION_COMMENTS = "/v1/questions/{questionId}/comments";
    public static final String QUESTION_VOTE = "/v1/questions/{questionId}/vote";
    public static final String COMMENT_VOTE = "/v1/comments/{commentId}/vote";
    public static final String QUESTION_BOOKMARK = "/v1/questions/{questionId}/bookmark";

    // Translation Endpoints
    public static final String TRANSLATION_LOOKUP = "/v1/translations/lookup";
    public static final String TRANSLATION_GLOSSARY = "/v1/translations/glossary";

    // Explore Endpoints
    public static final String EXPLORE_PLACES = "/v1/explore/places";
    public static final String EXPLORE_PLACE_DETAIL = "/v1/explore/places/{placeId}";
    public static final String EXPLORE_PROVINCE = "/v1/explore/province";
    public static final String EXPLORE_CATEGORIES = "/v1/explore/categories";
    public static final String EXPLORE_PATH_PATTERN = "/v1/explore/**";
}
