package com.backend.rootly.repository;

import com.backend.rootly.entity.UserReg;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserReg,String> {

    Optional<UserReg> findByEmail(String email);
    //use optional because user might not exist
}
