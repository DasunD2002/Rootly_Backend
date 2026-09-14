package com.backend.rootly.service.impl;

import com.backend.rootly.dto.ResponseDTO;
import com.backend.rootly.dto.request.CreateMemoryRequestDTO;
import com.backend.rootly.dto.response.MemoryResponseDTO;
import com.backend.rootly.entity.Capsule;
import com.backend.rootly.entity.CapsuleEntry;
import com.backend.rootly.entity.UnlockCondition;
import com.backend.rootly.entity.UserReg;
import com.backend.rootly.enums.CapsuleEntryType;
import com.backend.rootly.enums.CapsuleStatus;
import com.backend.rootly.enums.NotificationType;
import com.backend.rootly.enums.UnlockConditionType;
import com.backend.rootly.repository.CapsuleEntryRepository;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryServiceImplTests {

    private CapsuleRepository capsuleRepository;
    private CapsuleEntryRepository capsuleEntryRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private MemoryServiceImpl memoryService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        capsuleRepository = mock(CapsuleRepository.class);
        capsuleEntryRepository = mock(CapsuleEntryRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        MessageSource messageSource = mock(MessageSource.class);
        ModelMapper modelMapper = new ModelMapper();
        ResponseGenerator responseGenerator = new ResponseGenerator(modelMapper, messageSource);

        memoryService = new MemoryServiceImpl(
                capsuleRepository,
                capsuleEntryRepository,
                userRepository,
                notificationService,
                responseGenerator,
                clock
        );
    }

    @Test
    void createMemorySuccessAndNotifiesCreator() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .creatorId("user-creator")
                .title("Family Memories")
                .allowMemberContributions(true)
                .contributorIds(List.of("user-contrib"))
                .build();

        UserReg author = new UserReg();
        author.setId("user-contrib");
        author.setName("Contributor Dev");

        CreateMemoryRequestDTO request = CreateMemoryRequestDTO.builder()
                .type(CapsuleEntryType.PHOTO)
                .title("Grandma Birthday")
                .description("Birthday party in 1985")
                .mediaUrl("https://s3.amazonaws.com/rootly/grandma.jpg")
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));
        when(userRepository.findById("user-contrib")).thenReturn(Optional.of(author));
        when(capsuleEntryRepository.save(any(CapsuleEntry.class))).thenAnswer(i -> {
            CapsuleEntry entry = i.getArgument(0);
            entry.setId("entry-1");
            return entry;
        });

        ResponseEntity<Object> response = memoryService.createMemory("cap-1", "user-contrib", request, Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(notificationService).createNotification(
                eq("user-creator"),
                eq("Memory Added"),
                any(String.class),
                eq(NotificationType.MEMORY_ADDED)
        );
    }

    @Test
    void getMemoriesLockedCapsuleHidesPayload() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .title("Sealed Capsule")
                .status(CapsuleStatus.LOCKED)
                .unlockCondition(UnlockCondition.builder()
                        .type(UnlockConditionType.DATE)
                        .date(Instant.parse("2027-01-01T00:00:00Z"))
                        .build())
                .build();

        CapsuleEntry entry = CapsuleEntry.builder()
                .id("entry-1")
                .capsuleId("cap-1")
                .contributorId("user-1")
                .type(CapsuleEntryType.LETTER)
                .title("Letter to my future self")
                .textContent("This is the secret content of the letter")
                .mediaUrl(null)
                .likesCount(2)
                .createdAt(Instant.now(clock))
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));
        when(capsuleEntryRepository.findByCapsuleIdOrderByCreatedAtDesc("cap-1")).thenReturn(List.of(entry));

        ResponseEntity<Object> response = memoryService.getMemories("cap-1", "user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        @SuppressWarnings("unchecked")
        List<MemoryResponseDTO> list = (List<MemoryResponseDTO>) dto.getData();
        assertThat(list).hasSize(1);
        MemoryResponseDTO item = list.get(0);
        assertThat(item.getIsLocked()).isTrue();
        assertThat(item.getMediaUrl()).isNull();
        assertThat(item.getTextContent()).contains("LOCKED");
        assertThat(item.getTitle()).isEqualTo("Letter to my future self");
    }

    @Test
    void getMemoriesUnlockedCapsuleRevealsPayload() {
        Capsule capsule = Capsule.builder()
                .id("cap-1")
                .title("Opened Capsule")
                .status(CapsuleStatus.OPEN)
                .unlockCondition(UnlockCondition.builder()
                        .type(UnlockConditionType.DATE)
                        .date(Instant.parse("2025-01-01T00:00:00Z"))
                        .build())
                .build();

        CapsuleEntry entry = CapsuleEntry.builder()
                .id("entry-1")
                .capsuleId("cap-1")
                .contributorId("user-1")
                .type(CapsuleEntryType.PHOTO)
                .title("Family Photo")
                .description("Gathering photo")
                .mediaUrl("https://s3.amazonaws.com/rootly/pic.jpg")
                .textContent(null)
                .likesCount(4)
                .createdAt(Instant.now(clock))
                .build();

        when(capsuleRepository.findById("cap-1")).thenReturn(Optional.of(capsule));
        when(capsuleEntryRepository.findByCapsuleIdOrderByCreatedAtDesc("cap-1")).thenReturn(List.of(entry));

        ResponseEntity<Object> response = memoryService.getMemories("cap-1", "user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        @SuppressWarnings("unchecked")
        List<MemoryResponseDTO> list = (List<MemoryResponseDTO>) dto.getData();
        assertThat(list).hasSize(1);
        MemoryResponseDTO item = list.get(0);
        assertThat(item.getIsLocked()).isFalse();
        assertThat(item.getMediaUrl()).isEqualTo("https://s3.amazonaws.com/rootly/pic.jpg");
        assertThat(item.getDescription()).isEqualTo("Gathering photo");
    }

    @Test
    void reactToMemorySuccess() {
        CapsuleEntry entry = CapsuleEntry.builder()
                .id("entry-1")
                .capsuleId("cap-1")
                .likesCount(5)
                .build();

        when(capsuleEntryRepository.findById("entry-1")).thenReturn(Optional.of(entry));
        when(capsuleEntryRepository.save(any(CapsuleEntry.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<Object> response = memoryService.reactToMemory("cap-1", "entry-1", "user-1", Locale.ENGLISH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseDTO dto = (ResponseDTO) response.getBody();
        assertThat(dto.getResponseCode()).isEqualTo(ResponseCode.MEMORY_REACT_SUCCESS);
        assertThat(dto.getData()).isEqualTo(6);
    }
}
