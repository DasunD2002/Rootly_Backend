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

    // Explore Response Codes
    public static final String EXPLORE_PLACES_SUCCESS = "rootly-301";
    public static final String EXPLORE_CATEGORIES_SUCCESS = "rootly-302";
    public static final String PLACES_UNAVAILABLE = "rootly-303";

    // Question Forum Response Codes
    public static final String QUESTION_LIST_SUCCESS = "rootly-501";
    public static final String QUESTION_DETAIL_SUCCESS = "rootly-502";
    public static final String QUESTION_CREATE_SUCCESS = "rootly-503";
    public static final String QUESTION_COMMENT_CREATE_SUCCESS = "rootly-504";
    public static final String QUESTION_VOTE_SUCCESS = "rootly-505";
    public static final String QUESTION_BOOKMARK_SUCCESS = "rootly-506";
    public static final String QUESTION_UPDATE_SUCCESS = "rootly-507";
    public static final String QUESTION_DELETE_SUCCESS = "rootly-508";
    public static final String QUESTION_COMMENT_UPDATE_SUCCESS = "rootly-509";
    public static final String QUESTION_COMMENT_DELETE_SUCCESS = "rootly-510";

    // Translation Response Codes
    public static final String TRANSLATION_LOOKUP_SUCCESS = "rootly-601";
    public static final String TRANSLATION_GLOSSARY_SUCCESS = "rootly-602";

    // Post Response Codes
    public static final String POST_CREATE_SUCCESS = "rootly-601";
    public static final String POST_UPDATE_SUCCESS = "rootly-602";
    public static final String POST_DELETE_SUCCESS = "rootly-603";
    public static final String POST_NOT_FOUND = "rootly-604";
    public static final String POST_FORBIDDEN = "rootly-605";
    public static final String POST_GET_SUCCESS = "rootly-606";

    // Common & Validation Codes
    public static final String REQUIRED_DATA_ELEMENT_MISSING = "rootly-400";
    public static final String BAD_REQUEST = "rootly-400";
    public static final String UNAUTHORIZED = "rootly-401";
    public static final String FORBIDDEN = "rootly-403";
    public static final String NOT_FOUND = "rootly-404";
    public static final String CONFLICT = "rootly-409";
    public static final String INTERNAL_SERVER_ERROR = "rootly-500";
}
