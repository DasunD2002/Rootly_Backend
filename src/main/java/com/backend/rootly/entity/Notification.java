package com.backend.rootly.entity;

import com.backend.rootly.enums.NotificationType;
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
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @Indexed
    @Field("userId")
    private String userId;

    @Field("title")
    private String title;

    @Field("message")
    private String message;

    @Field("type")
    private NotificationType type;

    @Field("isRead")
    private Boolean isRead;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;
}
