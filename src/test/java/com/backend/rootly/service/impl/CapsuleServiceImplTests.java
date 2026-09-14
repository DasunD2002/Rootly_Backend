package com.backend.rootly.service.impl;

import com.backend.rootly.domain.CreateCapsuleDomain;
import com.backend.rootly.domain.InviteContributorDomain;
import com.backend.rootly.dto.ErrorResponse;
import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.request.InviteRequestDTO;
import com.backend.rootly.dto.request.UpdateCapsuleRequestDTO;
import com.backend.rootly.dto.response.CapsuleDetailResponseDTO;
import com.backend.rootly.dto.response.CapsuleResponseDTO;
import com.backend.rootly.dto.response.InviteResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.CapsuleEntry;
import com.backend.rootly.entity.CapsuleInvite;
import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsulePrivacy;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import com.backend.rootly.enums.InviteStatus;
import com.backend.rootly.enums.UnlockConditionType;
import com.backend.rootly.repository.CapsuleEntryRepository;
import com.backend.rootly.repository.CapsuleInviteRepository;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.NotificationService;
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
import java.time.ZoneOffset;
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
    private CapsuleEntryRepository capsuleEntryRepository;
    private UserRepository userRepository;
    private CapsuleInviteRepository capsuleInviteRepository;
    private NotificationService notificationService;
    private CapsuleServiceImpl capsuleService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        capsuleRepository = mock(CapsuleRepository.class);
        capsuleEntryRepository = mock(CapsuleEntryRepository.class);
        userRepository = mock(UserRepository.class);
        capsuleInviteRepository = mock(CapsuleInviteRepository.class);
        notificationService = mock(NotificationService.class);
        clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        capsuleService = new CapsuleServiceImpl(
                capsuleRepository,
                capsuleEntryRepository,
                userRepository,
                capsuleInviteRepository,
                notificationService,
                responseGenerator,
                modelMapper,
                clock
        );
    }

    @Test
    void createCapsuleSuccess() {
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
                .createdAt(Instant.now(clock))
                .updatedAt(Instant.now(clock))
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
    void createCapsuleSharedPrivacyWithoutUsersReturnsBadRequest() {
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
    void getCapsuleDetailWhenLockedRedactsMemories() {
        Instant futureUnlock = Instant.parse("2026-10-01T00:00:00Z");
        UnlockCondition condition = UnlockCondition.builder()
                .type(UnlockConditionType.DATE)
                .date(futureUnlock)
                .build();

        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .title("Locked Family Capsule")
                .status(CapsuleStatus.LOCKED)
                .unlockCondition(condition)
                .build();

        UserReg creator = new UserReg();
        creator.setId("user-1");
        creator.setName("Kumari Devi");

        CapsuleEntry entry = CapsuleEntry.builder()
                .id("mem-1")
                .capsuleId("cap-1")
                .contributorId("user-1")
                .mediaUrl("https://s3.amazonaws.com/rootly/photo.jpg")
                .textContent("Secret letter content")
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(creator));
        when(capsuleEntryRepository.findByCapsuleIdOrderByCreatedAtDesc("cap-1")).thenReturn(List.of(entry));

        ResponseEntity<Object> response = capsuleService.getCapsuleDetail("cap-1", "user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO body = (ResponseDTO) response.getBody();
        CapsuleDetailResponseDTO detail = (CapsuleDetailResponseDTO) body.getData();
        assertThat(detail.getStatus()).isEqualTo(CapsuleStatus.LOCKED);
        assertThat(detail.getCountdown()).contains("days");
        assertThat(detail.getMemories()).hasSize(1);
        // Security assertion: payload is redacted
        assertThat(detail.getMemories().get(0).getMediaUrl()).isNull();
        assertThat(detail.getMemories().get(0).getTextContent()).contains("LOCKED");
        assertThat(detail.getMemories().get(0).getIsLocked()).isTrue();
    }

    @Test
    void updateCapsuleSuccess() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .title("Old Title")
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));
        when(capsuleRepository.save(any(Capsule.class))).thenAnswer(i -> i.getArgument(0));

        UpdateCapsuleRequestDTO update = UpdateCapsuleRequestDTO.builder()
                .title("New Title")
                .allowMemberContributions(false)
                .build();

        ResponseEntity<Object> response = capsuleService.updateCapsule("cap-1", "user-1", update, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(capsuleRepository).save(any(Capsule.class));
    }

    @Test
    void deleteCapsuleSuccess() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));

        ResponseEntity<Object> response = capsuleService.deleteCapsule("cap-1", "user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(capsuleEntryRepository).deleteByCapsuleId("cap-1");
        verify(capsuleRepository).delete(capsule);
    }

    @Test
    void createInviteLinkSuccess() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .title("Family Time Capsule")
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));

        InviteRequestDTO inviteReq = InviteRequestDTO.builder()
                .inviteeEmail("cousin@rootly.app")
                .build();

        ResponseEntity<Object> response = capsuleService.createInvite("cap-1", "user-1", inviteReq, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        InviteResponseDTO inviteRes = (InviteResponseDTO) dto.getData();
        assertThat(inviteRes.getInviteLink()).startsWith("https://rootly.app/invite/");
        assertThat(inviteRes.getStatus()).isEqualTo(InviteStatus.PENDING);
        verify(capsuleInviteRepository).save(any(CapsuleInvite.class));
    }

    @Test
    void joinByInviteSuccess() {
        CapsuleInvite invite = CapsuleInvite.builder()
                .capsuleId("cap-1")
                .inviteToken("token-123")
                .status(InviteStatus.PENDING)
                .expiresAt(Instant.now(clock).plusSeconds(3600))
                .build();

        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .contributorIds(new ArrayList<>())
                .build();

        when(capsuleInviteRepository.findByInviteToken("token-123")).thenReturn(Optional.of(invite));
        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));

        ResponseEntity<Object> response = capsuleService.joinByInvite("token-123", "user-2", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(invite.getStatus()).isEqualTo(InviteStatus.ACCEPTED);
        assertThat(capsule.getContributorIds()).contains("user-2");
        verify(capsuleRepository).save(capsule);
        verify(capsuleInviteRepository).save(invite);
    }

    @Test
    void inviteContributorSuccess() {
        InviteContributorDomain request = InviteContributorDomain.builder()
                .contributorId("user-2")
                .build();

        Capsule existing = Capsule.builder()
                .id("capsule-1")
                .creatorId("user-1")
                .title("Family Capsule")
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
    void inviteContributorNonExistentCapsuleReturnsNotFound() {
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
}
