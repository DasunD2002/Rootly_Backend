package com.backend.rootly.service.impl;

import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.response.DashboardStatsResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.CapsuleType;
import com.backend.rootly.enums.UnlockConditionType;
import com.backend.rootly.repository.CapsuleEntryRepository;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.repository.UserRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTests {

    private UserRepository userRepository;
    private CapsuleRepository capsuleRepository;
    private CapsuleEntryRepository capsuleEntryRepository;
    private UserServiceImpl userService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        capsuleRepository = mock(CapsuleRepository.class);
        capsuleEntryRepository = mock(CapsuleEntryRepository.class);
        clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        userService = new UserServiceImpl(
                userRepository,
                capsuleRepository,
                capsuleEntryRepository,
                responseGenerator,
                modelMapper,
                clock
        );
    }

    @Test
    void getDashboardStatsSuccess() {
        UserReg user = new UserReg();
        user.setId("user-1");
        user.setName("Kumari Devi");
        user.setEmail("kumari@example.com");
        user.setSubscriptionMonthsLeft(10);

        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-1")
                .title("Heritage Time Capsule")
                .type(CapsuleType.FAMILY)
                .status(CapsuleStatus.LOCKED)
                .unlockCondition(UnlockCondition.builder()
                        .type(UnlockConditionType.DATE)
                        .date(Instant.parse("2026-12-31T00:00:00Z"))
                        .build())
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(capsuleRepository.countByCreatorIdOrContributorIdsContaining("user-1", "user-1")).thenReturn(3L);
        when(capsuleEntryRepository.countByContributorId("user-1")).thenReturn(15L);
        when(capsuleRepository.findByCreatorIdOrContributorIdsContainingOrderByCreatedAtDesc("user-1", "user-1"))
                .thenReturn(List.of(capsule));
        when(capsuleEntryRepository.countByCapsuleId("cap-1")).thenReturn(5L);

        ResponseEntity<Object> response = userService.getDashboardStats("user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.RSP_SUCCESS);
        DashboardStatsResponseDTO stats = (DashboardStatsResponseDTO) dto.getData();
        assertThat(stats.getName()).isEqualTo("Kumari Devi");
        assertThat(stats.getCapsuleCount()).isEqualTo(3L);
        assertThat(stats.getMemoryCount()).isEqualTo(15L);
        assertThat(stats.getSubscriptionMonthsLeft()).isEqualTo(10);
        assertThat(stats.getRecentCapsules()).hasSize(1);
        assertThat(stats.getRecentCapsules().get(0).getMemoryCount()).isEqualTo(5L);
    }
}
