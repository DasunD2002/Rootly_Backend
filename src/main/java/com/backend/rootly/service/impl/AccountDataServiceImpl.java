package com.backend.rootly.service.impl;

import com.backend.rootly.dto.request.AccountDataRequestDTO;
import com.backend.rootly.entity.AccountData;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.AccountDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccountDataServiceImpl implements AccountDataService {
    private static final int MAX_DOCUMENT_LENGTH = 500_000;
    private static final int MAX_DEPTH = 20;
    private static final int MAX_ITEMS = 1_000;
    private static final int MAX_FIELD_LENGTH = 200;
    private static final int MAX_TEXT_LENGTH = 30_000;
    private static final Set<String> KEYS = Set.of("journey", "field-notes", "search-history", "social", "preferences", "translations");
    private final MongoTemplate mongoTemplate;

    @Override
    public AccountData read(String key, UserReg user) {
        String id = identity(key, user);
        AccountData document = mongoTemplate.findById(id, AccountData.class);
        if (document == null) {
            document = new AccountData();
            document.setData(Map.of());
        }
        return document;
    }

    @Override
    public AccountData save(String key, AccountDataRequestDTO request, UserReg user) {
        String id = identity(key, user);
        validateData(request.data(), 0);
        if (request.data().toString().length() > MAX_DOCUMENT_LENGTH) {
            throw new IllegalArgumentException("Saved data is too large.");
        }
        Query query = Query.query(Criteria.where("_id").is(id).and("version").is(request.version()));
        Update update = new Update().set("userId", user.getId()).set("key", key)
                .set("data", request.data()).set("updatedAt", Instant.now()).inc("version", 1);
        AccountData saved = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), AccountData.class);
        if (saved != null) {
            return saved;
        }
        if (request.version() == 0) {
            AccountData initial = new AccountData();
            initial.setId(id);
            initial.setUserId(user.getId());
            initial.setKey(key);
            initial.setData(request.data());
            initial.setVersion(1);
            initial.setUpdatedAt(Instant.now());
            try {
                return mongoTemplate.insert(initial);
            } catch (DuplicateKeyException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This data changed in another session. Reload it before saving again.", exception);
            }
        }
        throw conflict();
    }

    private static String identity(String key, UserReg user) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        if (!KEYS.contains(key)) {
            throw new IllegalArgumentException("Unsupported saved-data key.");
        }
        return user.getId() + ":" + key;
    }

    private static ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "This data changed in another session. Reload it before saving again.");
    }


    private static void validateData(Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Saved data is nested too deeply.");
        }
        if (value instanceof Map<?, ?> map) {
            validateMap(map, depth);
        } else if (value instanceof List<?> list) {
            requireItemLimit(list.size());
            for (Object item : list) {
                validateData(item, depth + 1);
            }
        } else if (value instanceof String text && text.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("Saved text is too long.");
        }
    }

    private static void validateMap(Map<?, ?> map, int depth) {
        requireItemLimit(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String name = entry.getKey().toString();
            if (name.startsWith("$") || name.contains(".") || name.length() > MAX_FIELD_LENGTH) {
                throw new IllegalArgumentException("Invalid saved-data field.");
            }
            validateData(entry.getValue(), depth + 1);
        }
    }

    private static void requireItemLimit(int size) {
        if (size > MAX_ITEMS) {
            throw new IllegalArgumentException("Too many saved items.");
        }
    }
}
