package com.backend.rootly.service.impl;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.domain.UpdateCapsuleDomain;
import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.response.CapsuleResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapsuleServiceImplTests {

    private CapsuleRepository capsuleRepository;
    private CapsuleServiceImpl capsuleService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        capsuleRepository = mock(CapsuleRepository.class);
        clock = Clock.systemUTC();
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        capsuleService = new CapsuleServiceImpl(capsuleRepository, responseGenerator, modelMapper, clock);
    }

    @Test
    void createCapsuleSuccess() throws Exception {
        CreateCapsuleDomain request = CreateCapsuleDomain.builder()
                .creatorId("user-1")
                .title("My Capsule")
                .description("Capsule description")
                .type(CapsuleType.PERSONAL)
                .privacy(CapsulePrivacy.PRIVATE)
                .build();

        Capsule saved = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .title("My Capsule")
                .status(CapsuleStatus.OPEN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(capsuleRepository.save(any(Capsule.class))).thenReturn(saved);

        ResponseEntity<Object> response = capsuleService.createCapsule(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.CAPSULE_CREATE_SUCCESS);
        assertThat(dto.getData()).isInstanceOf(CapsuleResponseDTO.class);
        CapsuleResponseDTO capsuleDto = (CapsuleResponseDTO) dto.getData();
        assertThat(capsuleDto.getId()).isEqualTo("capsule-1");
    }

    @Test
    void createCapsuleSharedPrivacyWithoutUsersReturnsBadRequest() throws Exception {
        CreateCapsuleDomain request = CreateCapsuleDomain.builder()
                .creatorId("user-1")
                .type(CapsuleType.PERSONAL)
                .privacy(CapsulePrivacy.SHARED)
                .sharedWithUserIds(List.of())
                .build();

        ResponseEntity<Object> response = capsuleService.createCapsule(request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.BAD_REQUEST);
    }

    @Test
    void inviteContributorSuccess() throws Exception {
        InviteContributorDomain request = InviteContributorDomain.builder()
                .contributorId("user-2")
                .build();

        Capsule existing = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .status(CapsuleStatus.OPEN)
                .contributorIds(new ArrayList<>())
                .build();

        when(capsuleRepository.findById("capsule-1")).thenReturn(Optional.of(existing));
        when(capsuleRepository.save(any(Capsule.class))).thenReturn(existing);

        ResponseEntity<Object> response = capsuleService.inviteContributor("capsule-1", request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.CAPSULE_INVITE_SUCCESS);
        verify(capsuleRepository).save(existing);
    }

    @Test
    void inviteContributorNonExistentCapsuleReturnsNotFound() throws Exception {
        InviteContributorDomain request = InviteContributorDomain.builder()
                .contributorId("user-2")
                .build();

        when(capsuleRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = capsuleService.inviteContributor("non-existent", request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.CAPSULE_NOT_FOUND);
    }

    @Test
    void getCapsuleSuccess() {
        Capsule existing = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .title("My Capsule")
                .status(CapsuleStatus.OPEN)
                .build();

        when(capsuleRepository.findById("capsule-1")).thenReturn(Optional.of(existing));

        ResponseEntity<Object> response = capsuleService.getCapsule("capsule-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.CAPSULE_GET_SUCCESS);
        assertThat(dto.getData()).isInstanceOf(CapsuleResponseDTO.class);
        CapsuleResponseDTO capsuleDto = (CapsuleResponseDTO) dto.getData();
        assertThat(capsuleDto.getId()).isEqualTo("capsule-1");
    }

    @Test
    void getCapsuleNotFoundReturns404() {
        when(capsuleRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = capsuleService.getCapsule("non-existent", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.CAPSULE_NOT_FOUND);
    }

    @Test
    void updateCapsuleSuccess() {
        UpdateCapsuleDomain request = UpdateCapsuleDomain.builder()
                .title("Updated Title")
                .description("Updated Desc")
                .type(CapsuleType.FAMILY)
                .build();

        Capsule existing = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .title("Old Title")
                .status(CapsuleStatus.OPEN)
                .build();

        when(capsuleRepository.findById("capsule-1")).thenReturn(Optional.of(existing));
        when(capsuleRepository.save(any(Capsule.class))).thenReturn(existing);

        ResponseEntity<Object> response = capsuleService.updateCapsule("capsule-1", request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.CAPSULE_UPDATE_SUCCESS);
        verify(capsuleRepository).save(existing);
        assertThat(existing.getTitle()).isEqualTo("Updated Title");
        assertThat(existing.getDescription()).isEqualTo("Updated Desc");
        assertThat(existing.getType()).isEqualTo(CapsuleType.FAMILY);
    }

    @Test
    void updateCapsuleNotFoundReturns404() {
        UpdateCapsuleDomain request = UpdateCapsuleDomain.builder()
                .title("Updated Title")
                .build();

        when(capsuleRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = capsuleService.updateCapsule("non-existent", request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.CAPSULE_NOT_FOUND);
    }

    @Test
    void deleteCapsuleSuccess() {
        Capsule existing = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .status(CapsuleStatus.OPEN)
                .build();

        when(capsuleRepository.findById("capsule-1")).thenReturn(Optional.of(existing));

        ResponseEntity<Object> response = capsuleService.deleteCapsule("capsule-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ResponseDTO.class);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.CAPSULE_DELETE_SUCCESS);
        verify(capsuleRepository).delete(existing);
    }

    @Test
    void deleteCapsuleNotFoundReturns404() {
        when(capsuleRepository.findById("non-existent")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = capsuleService.deleteCapsule("non-existent", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isInstanceOf(ErrorResponse.class);
        ErrorResponse err = (ErrorResponse) response.getBody();
        assertThat(err.getErrorCode()).isEqualTo(ResponseCode.CAPSULE_NOT_FOUND);
    }
}
