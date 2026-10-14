package com.backend.rootly.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostComment {

    @Field("userId")
    private String userId;

    @Field("text")
    private String text;

    @Field("createdAt")
    private Instant createdAt;
}
