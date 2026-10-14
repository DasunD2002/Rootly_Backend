package com.backend.rootly.controller;

import com.backend.rootly.dto.request.AccountDataRequestDTO;
import com.backend.rootly.entity.AccountData;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.service.AccountDataService;
import com.backend.rootly.utility.EndPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping(EndPoint.API)
@CrossOrigin
@RequiredArgsConstructor
public class AccountDataController {
    private final AccountDataService accountDataService;

    @GetMapping(EndPoint.ACCOUNT_DATA)
    public ResponseEntity<Object> read(@PathVariable String key, @AuthenticationPrincipal UserReg user) {
        return response(accountDataService.read(key, user));
    }

    @PutMapping(EndPoint.ACCOUNT_DATA)
    public ResponseEntity<Object> save(@PathVariable String key,
                                      @Validated @RequestBody AccountDataRequestDTO request,
                                      @AuthenticationPrincipal UserReg user) {
        return response(accountDataService.save(key, request, user));
    }

    private static ResponseEntity<Object> response(AccountData saved) {
        return ResponseEntity.ok(Map.of("version", saved.getVersion(), "data", saved.getData()));
    }
}
