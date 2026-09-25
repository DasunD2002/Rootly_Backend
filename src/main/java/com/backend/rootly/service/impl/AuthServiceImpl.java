package com.backend.rootly.service.impl;

import com.backend.rootly.domain.UserLogin;
import com.backend.rootly.domain.UserRegister;
import com.backend.rootly.dto.response.LoginResponseDTO;
import com.backend.rootly.dto.response.RegisterResponseDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.AuthService;
import com.backend.rootly.service.security.JwtService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ResponseGenerator responseGenerator;

    @Override
    @Transactional
    public ResponseEntity<Object> register(UserRegister request, Locale locale) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.CONFLICT,
                    ResponseCode.EMAIL_ALREADY_EXISTS, MessageConstant.EMAIL_ALREADY_EXISTS, locale);
        }

        UserReg user = new UserReg();
        user.setName(request.getName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setDistrict(request.getDistrict());
        user.setLanguages(request.getLanguages());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setPhotoUrl(request.getPhotoUrl());

        // Default business values
        user.setRole("USER");
        user.setVerificationStatus("PENDING");
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setIsOtpVerified(false);
        user.setProfileVisibility("PUBLIC");
        user.setShowActivity(true);

        UserReg savedUser = userRepository.save(user);

        if (log.isInfoEnabled()) {
            log.info("User registered successfully with id: {}", savedUser.getId());
        }

        RegisterResponseDTO responseDTO = RegisterResponseDTO.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .verificationStatus(savedUser.getVerificationStatus())
                .build();

        return responseGenerator.generateSuccessResponse(request, HttpStatus.CREATED,
                ResponseCode.AUTH_REGISTER_SUCCESS, MessageConstant.USER_REGISTER_SUCCESS, locale, responseDTO);
    }

    @Override
    public ResponseEntity<Object> authenticate(UserLogin request, Locale locale) {
        UserReg user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return responseGenerator.generateErrorResponse(request, HttpStatus.UNAUTHORIZED,
                    ResponseCode.INVALID_CREDENTIALS, MessageConstant.INVALID_CREDENTIALS, locale);
        }

        String token = jwtService.generateToken(user.getEmail());

        if (log.isInfoEnabled()) {
            log.info("Authentication successful for email: {}", user.getEmail());
        }

        LoginResponseDTO responseDTO = LoginResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .verificationStatus(user.getVerificationStatus())
                .token(token)
                .build();

        return responseGenerator.generateSuccessResponse(request, HttpStatus.OK,
                ResponseCode.AUTH_LOGIN_SUCCESS, MessageConstant.AUTH_LOGIN_SUCCESS, locale, responseDTO);
    }
}
