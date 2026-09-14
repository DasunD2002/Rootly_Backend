package com.backend.rootly.entity;

import com.backend.rootly.enums.InviteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "capsuleInvites")
public class CapsuleInvite {

    @Id
    private String id;

    @Indexed
    @Field("capsuleId")
    private String capsuleId;

    @Indexed(unique = true)
    @Field("inviteToken")
    private String inviteToken;

    @Field("inviterId")
    private String inviterId;

    @Field("inviteeEmail")
    private String inviteeEmail;

    @Field("status")
    private InviteStatus status;

    @Field("expiresAt")
    private Instant expiresAt;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;
}
