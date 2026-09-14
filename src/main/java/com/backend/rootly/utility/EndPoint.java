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
    public static final String USER_DASHBOARD_STATS = "/v1/user/dashboard-stats";

    // Memory Endpoints
    public static final String CAPSULE_MEMORIES = "/v1/capsules/{id}/memories";
    public static final String CAPSULE_MEMORY_REACT = "/v1/capsules/{id}/memories/{memoryId}/react";

    // Media Endpoints
    public static final String MEDIA_UPLOAD = "/v1/media/upload";

    // Notification Endpoints
    public static final String NOTIFICATIONS = "/v1/notifications";
    public static final String NOTIFICATIONS_MARK_READ = "/v1/notifications/mark-read";

    // Explore Endpoints
    public static final String EXPLORE_PLACES = "/v1/explore/places";
    public static final String EXPLORE_CATEGORIES = "/v1/explore/categories";
    public static final String EXPLORE_PATH_PATTERN = "/v1/explore/**";
}
