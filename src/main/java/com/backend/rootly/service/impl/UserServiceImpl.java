package com.backend.rootly.service.impl;

import com.backend.rootly.dto.response.UserResponseDTO;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.UserService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ResponseGenerator responseGenerator;
    private final ModelMapper modelMapper;
    private final UserFollowService userFollowService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    public ResponseEntity<Object> getUserById(String userId, Locale locale) {
        UserReg user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }
        UserResponseDTO responseDTO = modelMapper.map(user, UserResponseDTO.class);
        responseDTO.setFollowerCount(Math.toIntExact(userFollowService.countFollowers(userId)));
        responseDTO.setFollowingCount(Math.toIntExact(userFollowService.countFollowing(userId)));
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserReg viewer)
                || !userId.equals(viewer.getId())) {
            responseDTO.setEmail(null);
            responseDTO.setPhone(null);
        }
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_GET, responseDTO);
    }

    @Override
    public ResponseEntity<Object> updateUserProfile(String userId, com.backend.rootly.dto.request.UserUpdateRequestDTO request, Locale locale) {
        requireOwner(userId);
        UserReg user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getHandle() != null) user.setHandle(request.getHandle());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getDistrict() != null) user.setDistrict(request.getDistrict());
        if (request.getPhotoUrl() != null) user.setPhotoUrl(request.getPhotoUrl());
        if (request.getCoverUrl() != null) user.setCoverUrl(request.getCoverUrl());

        user = userRepository.save(user);

        UserResponseDTO responseDTO = modelMapper.map(user, UserResponseDTO.class);
        responseDTO.setFollowerCount(Math.toIntExact(userFollowService.countFollowers(userId)));
        responseDTO.setFollowingCount(Math.toIntExact(userFollowService.countFollowing(userId)));
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_UPDATE, responseDTO);
    }

    @Override
    public ResponseEntity<Object> changePassword(String userId, com.backend.rootly.dto.request.ChangePasswordRequestDTO request, Locale locale) {
        requireOwner(userId);
        UserReg user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.BAD_REQUEST,
                    ResponseCode.BAD_REQUEST, "Incorrect current password", locale);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, "Password changed successfully", null);
    }

    private static void requireOwner(String userId) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserReg user)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        if (!userId.equals(user.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only update your own account.");
        }
    }
}
