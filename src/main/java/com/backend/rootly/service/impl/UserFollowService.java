package com.backend.rootly.service.impl;

import com.backend.rootly.entity.UserFollow;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserFollowService {
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;

    public Map<String, Object> status(String target, UserReg user) {
        requireUser(user);
        if (!userRepository.existsById(target)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        UserFollow relation = mongoTemplate.findById(user.getId() + ":" + target, UserFollow.class);
        return Map.of("following", relation != null && relation.isActive(),
                "followerCount", countFollowers(target), "followingCount", countFollowing(target));
    }

    public Map<String, Object> update(String target, boolean following, UserReg user) {
        requireUser(user);
        if (target.equals(user.getId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself.");
        if (!userRepository.existsById(target)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.");
        Query query = Query.query(Criteria.where("_id").is(user.getId() + ":" + target));
        mongoTemplate.upsert(query, new Update().set("followerId", user.getId()).set("followedId", target)
                .set("active", following), UserFollow.class);
        return status(target, user);
    }

    public long countFollowers(String id) {
        return mongoTemplate.count(Query.query(Criteria.where("followedId").is(id).and("active").is(true)), UserFollow.class);
    }

    public long countFollowing(String id) {
        return mongoTemplate.count(Query.query(Criteria.where("followerId").is(id).and("active").is(true)), UserFollow.class);
    }

    private static void requireUser(UserReg user) {
        if (user == null || user.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
    }
}
