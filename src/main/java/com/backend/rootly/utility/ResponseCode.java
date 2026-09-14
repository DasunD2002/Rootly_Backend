package com.backend.rootly.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.DataClass")
public class ResponseCode {

    public static final String RSP_SUCCESS = "00";
    public static final String RSP_FAILED = "01";
    public static final String RSP_ERROR = "06";

    // Auth Response Codes
    public static final String AUTH_REGISTER_SUCCESS = "rootly-101";
    public static final String AUTH_LOGIN_SUCCESS = "rootly-102";
    public static final String USER_NOT_FOUND = "rootly-103";
    public static final String INVALID_CREDENTIALS = "rootly-104";
    public static final String EMAIL_ALREADY_EXISTS = "rootly-105";

    // Capsule Response Codes
    public static final String CAPSULE_CREATE_SUCCESS = "rootly-201";
    public static final String CAPSULE_INVITE_SUCCESS = "rootly-202";
    public static final String CAPSULE_NOT_FOUND = "rootly-203";
    public static final String CAPSULE_GET_SUCCESS = "rootly-204";
    public static final String CAPSULE_UPDATE_SUCCESS = "rootly-205";
    public static final String CAPSULE_DELETE_SUCCESS = "rootly-206";

    // Memory & Media Response Codes
    public static final String MEMORY_CREATE_SUCCESS = "rootly-207";
    public static final String MEMORY_GET_SUCCESS = "rootly-208";
    public static final String MEDIA_UPLOAD_SUCCESS = "rootly-209";
    public static final String MEMORY_REACT_SUCCESS = "rootly-212";
    public static final String MEMORY_NOT_FOUND = "rootly-213";

    // Notification Response Codes
    public static final String NOTIFICATION_GET_SUCCESS = "rootly-210";
    public static final String NOTIFICATION_UPDATE_SUCCESS = "rootly-211";

    // Explore Response Codes
    public static final String EXPLORE_PLACES_SUCCESS = "rootly-301";
    public static final String EXPLORE_CATEGORIES_SUCCESS = "rootly-302";
    public static final String PLACES_UNAVAILABLE = "rootly-303";

    // Common & Validation Codes
    public static final String REQUIRED_DATA_ELEMENT_MISSING = "rootly-400";
    public static final String BAD_REQUEST = "rootly-400";
    public static final String UNAUTHORIZED = "rootly-401";
    public static final String FORBIDDEN = "rootly-403";
    public static final String NOT_FOUND = "rootly-404";
    public static final String CONFLICT = "rootly-409";
    public static final String INTERNAL_SERVER_ERROR = "rootly-500";
}
