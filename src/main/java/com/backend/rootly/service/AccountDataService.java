package com.backend.rootly.service;

import com.backend.rootly.dto.request.AccountDataRequestDTO;
import com.backend.rootly.entity.AccountData;
import com.backend.rootly.entity.UserReg;

public interface AccountDataService {
    AccountData read(String key, UserReg user);
    AccountData save(String key, AccountDataRequestDTO request, UserReg user);
}
