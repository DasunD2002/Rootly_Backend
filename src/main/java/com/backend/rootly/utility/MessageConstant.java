package com.backend.rootly.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("PMD.DataClass")
public class MessageConstant {

    /*--------------- Common Messages ---------------*/
    public static final String SYSTEM_ERROR = "system.error";
    public static final String SUCCESSFULLY_SAVE = "val.success.add";
    public static final String SUCCESSFULLY_GET = "val.success.get";
    public static final String SUCCESSFULLY_UPDATE = "val.success.update";
    public static final String SUCCESSFULLY_DELETE = "val.success.delete";
    public static final String BAD_REQUEST = "val.bad.request";
    public static final String UNAUTHORIZED = "val.unauthorized";

    /*--------------- Auth Messages ---------------*/
    public static final String USER_REGISTER_SUCCESS = "val.user.register.success";
    public static final String AUTH_LOGIN_SUCCESS = "val.auth.login.success";
    public static final String USER_NOT_FOUND = "val.user.not.found";
    public static final String INVALID_CREDENTIALS = "val.auth.invalid.credentials";
    public static final String EMAIL_ALREADY_EXISTS = "val.user.email.already.exists";

    /*--------------- Capsule Messages ---------------*/
    public static final String CAPSULE_CREATE_SUCCESS = "val.capsule.create.success";
    public static final String CAPSULE_INVITE_SUCCESS = "val.capsule.invite.success";
    public static final String CAPSULE_NOT_FOUND = "val.capsule.not.found";
    public static final String CAPSULE_ALREADY_EXISTS = "val.capsule.already.exists";
    public static final String CAPSULE_UPDATE_SUCCESS = "val.capsule.update.success";
    public static final String CAPSULE_DELETE_SUCCESS = "val.capsule.delete.success";

    /*--------------- Explore Messages ---------------*/
    public static final String PLACES_UNAVAILABLE = "val.places.unavailable";
    public static final String EXPLORE_PLACES_SUCCESS = "val.explore.places.success";
    public static final String EXPLORE_CATEGORIES_SUCCESS = "val.explore.categories.success";
}
