package com.backend.rootly.service.impl;

import com.backend.rootly.dto.response.DashboardStatsResponseDTO;
import com.backend.rootly.dto.response.RecentCapsuleDTO;
import com.backend.rootly.dto.response.UserResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.repository.CapsuleEntryRepository;
import com.backend.rootly.repository.CapsuleRepository;
import com.backend.rootly.repository.UserRepository;
import com.backend.rootly.service.UserService;
import com.backend.rootly.utility.MessageConstant;
import com.backend.rootly.utility.ResponseCode;
import com.backend.rootly.utility.ResponseGenerator;
import com.backend.rootly.utility.TimeUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleEntryRepository capsuleEntryRepository;
    private final ResponseGenerator responseGenerator;
    private final ModelMapper modelMapper;
    private final Clock clock;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           CapsuleRepository capsuleRepository,
                           CapsuleEntryRepository capsuleEntryRepository,
                           ResponseGenerator responseGenerator,
                           ModelMapper modelMapper,
                           @Autowired(required = false) Clock clock) {
        this.userRepository = userRepository;
        this.capsuleRepository = capsuleRepository;
        this.capsuleEntryRepository = capsuleEntryRepository;
        this.responseGenerator = responseGenerator;
        this.modelMapper = modelMapper;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    @Override
    public ResponseEntity<Object> getUserById(String userId, Locale locale) {
        UserReg user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }
        UserResponseDTO responseDTO = modelMapper.map(user, UserResponseDTO.class);
        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_GET, responseDTO);
    }

    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public ResponseEntity<Object> getDashboardStats(String userId, Locale locale) {
        UserReg user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return responseGenerator.generateErrorResponse(null, HttpStatus.NOT_FOUND,
                    ResponseCode.USER_NOT_FOUND, MessageConstant.USER_NOT_FOUND, locale);
        }

        Instant now = Instant.now(clock);
        long capsuleCount = capsuleRepository.countByCreatorIdOrContributorIdsContaining(userId, userId);
        long memoryCount = capsuleEntryRepository.countByContributorId(userId);
        int subscriptionMonths = user.getSubscriptionMonthsLeft() != null ? user.getSubscriptionMonthsLeft() : 12;

        List<Capsule> capsules = capsuleRepository.findByCreatorIdOrContributorIdsContainingOrderByCreatedAtDesc(userId, userId);
        List<RecentCapsuleDTO> recentCapsules = new ArrayList<>();

        int limit = Math.min(capsules.size(), 5);
        for (int i = 0; i < limit; i++) {
            Capsule c = capsules.get(i);
            Instant targetDate = c.getUnlockCondition() != null ? c.getUnlockCondition().getDate() : null;
            boolean isLocked = (c.getStatus() != null && c.getStatus().isLocked())
                    || (targetDate != null && now.isBefore(targetDate));

            long count = capsuleEntryRepository.countByCapsuleId(c.getId());

            recentCapsules.add(RecentCapsuleDTO.builder()
                    .id(c.getId())
                    .title(c.getTitle())
                    .category(c.getType())
                    .status(isLocked ? CapsuleStatus.LOCKED : c.getStatus())
                    .coverPhotoUrl(c.getCoverPhotoUrl())
                    .unlockDate(targetDate)
                    .countdown(TimeUtil.formatCountdown(targetDate, now))
                    .memoryCount(count)
                    .build());
        }

        DashboardStatsResponseDTO dashboardStats = DashboardStatsResponseDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .photoUrl(user.getPhotoUrl())
                .capsuleCount(capsuleCount)
                .memoryCount(memoryCount)
                .subscriptionMonthsLeft(subscriptionMonths)
                .recentCapsules(recentCapsules)
                .build();

        return responseGenerator.generateSuccessResponse(HttpStatus.OK,
                ResponseCode.RSP_SUCCESS, MessageConstant.SUCCESSFULLY_GET, dashboardStats);
    }
}
