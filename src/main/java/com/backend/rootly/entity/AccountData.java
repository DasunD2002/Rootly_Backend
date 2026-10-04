package com.backend.rootly.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

/** Private feature data; identity and ownership come from the authenticated user. */
@Data
@Document(collection = "account_data")
public class AccountData {
    @Id
    private String id;
    private String userId;
    private String key;
    private long version;
    private Map<String, Object> data;
    private Instant updatedAt;
}
