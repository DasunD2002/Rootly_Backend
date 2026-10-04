package com.backend.rootly.entity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document("user_follows")
public class UserFollow {
    @Id private String id;
    private String followerId;
    private String followedId;
    private boolean active;
}
