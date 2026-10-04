package com.backend.rootly.service;

import com.backend.rootly.dto.request.CreateCapsuleRequestDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.UserReg;
import java.util.List;

public interface CapsuleLibraryService {
    List<Capsule> list(UserReg user);
    Capsule update(String id, CreateCapsuleRequestDTO request, UserReg user);
    void delete(String id, UserReg user);
}
