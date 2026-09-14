package com.backend.rootly.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Document(collection = "users")
@SuppressWarnings("PMD.TooManyFields") //because have too many data
public class UserReg {

    @Id
    private String id;

    @NotNull
    @Field("name")
    private String name;

    @Email
    @NotNull
    @Field("email")
    private String email;

    @Size(min = 10, max = 10)
    @Field("phone")
    private String phone;

    @Field("gender")
    private String gender;

    @Field("password")
    private String password;

    @Field("photoUrl")
    private String photoUrl;

    @Field("district")
    private String district;

    @NotEmpty
    @Field("languages")
    private List<String> languages;

    @Field("role")
    private String role;

    @Field("verificationStatus")
    private String verificationStatus;

    @NotNull
    @Field("followerCount")
    private Integer followerCount;

    @NotNull
    @Field("followingCount")
    private Integer followingCount;

    @NotNull
    @Field("isOtpVerified")
    private Boolean isOtpVerified;

    @Field("profile_visibility")
    private String profileVisibility;

    @NotNull
    @Field("show_activity")
    private Boolean showActivity;

    @Field("subscription_months_left")
    private Integer subscriptionMonthsLeft;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}