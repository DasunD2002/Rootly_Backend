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

    // Explore Endpoints
    public static final String EXPLORE_PLACES = "/v1/explore/places";
    public static final String EXPLORE_CATEGORIES = "/v1/explore/categories";
    public static final String EXPLORE_PATH_PATTERN = "/v1/explore/**";

    // Post Endpoints
    public static final String POSTS = "/v1/posts";
    public static final String POST_DETAIL = "/v1/posts/{postId}";
}
