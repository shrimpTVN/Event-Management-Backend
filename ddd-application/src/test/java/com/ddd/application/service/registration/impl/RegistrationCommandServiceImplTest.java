package com.ddd.application.service.registration.impl;

import com.ddd.application.service.event.cache.RecommendationCacheService;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.exception.StudentProfileRequiredException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.Registration;
import com.ddd.domain.model.StudentProfile;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.application.mapper.RegistrationDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationCommandServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @Mock
    private EventPointRepository eventPointRepository;

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private StudentSemesterPointCommandService studentSemesterPointCommandService;

    @Mock
    private StudentCriteriaTransactionCommandService studentCriteriaTransactionCommandService;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RegistrationDtoMapper registrationDtoMapper;

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RecommendationCacheService recommendationCacheService;

    @InjectMocks
    private RegistrationCommandServiceImpl registrationCommandService;

    private Event createTestEvent(Long eventId, Long fanpageId) {
        Event event = new Event();
        event.setId(eventId);
        event.setFanpageId(fanpageId);
        event.setStatus(EventStatusEnum.PUBLISHED.name());
        event.setActive(true);
        event.setDateHappen(Instant.now().minus(1, ChronoUnit.HOURS));
        return event;
    }

    @Test
    @DisplayName("Should successfully mark attendees as ABSENT and save")
    void fanpageMemberBatchAbsent_success() {
        Long eventId = 10L;
        Long fanpageId = 20L;
        Long memberId = 1L;
        List<Long> studentUserIds = List.of(101L, 102L);

        Event event = createTestEvent(eventId, fanpageId);
        when(securityContextPort.getCurrentUserId()).thenReturn(memberId);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(fanpageMemberRepository.isActiveMember(fanpageId, memberId)).thenReturn(true);

        Registration reg1 = new Registration();
        reg1.setUserId(101L);
        reg1.setEventId(eventId);
        reg1.setStatus(RegistrationStatusEnum.REGISTERED);

        Registration reg2 = new Registration();
        reg2.setUserId(102L);
        reg2.setEventId(eventId);
        reg2.setStatus(RegistrationStatusEnum.CHECKED_IN);

        when(registrationRepository.findAllByUserIdsAndEvent(any(), eq(eventId))).thenReturn(List.of(reg1, reg2));

        assertDoesNotThrow(() -> registrationCommandService.fanpageMemberBatchAbsent(eventId, studentUserIds));

        assertEquals(RegistrationStatusEnum.ABSENT, reg1.getStatus());
        assertEquals(RegistrationStatusEnum.ABSENT, reg2.getStatus());
        verify(registrationRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should throw EventAttendanceException when a student has not registered")
    void fanpageMemberBatchAbsent_unregistered_shouldThrow() {
        Long eventId = 10L;
        Long fanpageId = 20L;
        Long memberId = 1L;
        List<Long> studentUserIds = List.of(101L, 102L);

        Event event = createTestEvent(eventId, fanpageId);
        when(securityContextPort.getCurrentUserId()).thenReturn(memberId);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(fanpageMemberRepository.isActiveMember(fanpageId, memberId)).thenReturn(true);

        Registration reg1 = new Registration();
        reg1.setUserId(101L);
        reg1.setStatus(RegistrationStatusEnum.REGISTERED);

        when(registrationRepository.findAllByUserIdsAndEvent(any(), eq(eventId))).thenReturn(List.of(reg1));

        assertThrows(EventAttendanceException.class, () ->
                registrationCommandService.fanpageMemberBatchAbsent(eventId, studentUserIds));
        verify(registrationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw EventAttendanceException when a student is already ABSENT")
    void fanpageMemberBatchAbsent_alreadyAbsent_shouldThrow() {
        Long eventId = 10L;
        Long fanpageId = 20L;
        Long memberId = 1L;
        List<Long> studentUserIds = List.of(101L);

        Event event = createTestEvent(eventId, fanpageId);
        when(securityContextPort.getCurrentUserId()).thenReturn(memberId);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(fanpageMemberRepository.isActiveMember(fanpageId, memberId)).thenReturn(true);

        Registration reg1 = new Registration();
        reg1.setUserId(101L);
        reg1.setStatus(RegistrationStatusEnum.ABSENT);

        when(registrationRepository.findAllByUserIdsAndEvent(any(), eq(eventId))).thenReturn(List.of(reg1));

        assertThrows(EventAttendanceException.class, () ->
                registrationCommandService.fanpageMemberBatchAbsent(eventId, studentUserIds));
    }

    @Test
    @DisplayName("Should throw EventAttendanceException when a student has already completed attendance")
    void fanpageMemberBatchAbsent_alreadyCompleted_shouldThrow() {
        Long eventId = 10L;
        Long fanpageId = 20L;
        Long memberId = 1L;
        List<Long> studentUserIds = List.of(101L);

        Event event = createTestEvent(eventId, fanpageId);
        when(securityContextPort.getCurrentUserId()).thenReturn(memberId);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(fanpageMemberRepository.isActiveMember(fanpageId, memberId)).thenReturn(true);

        Registration reg1 = new Registration();
        reg1.setUserId(101L);
        reg1.setStatus(RegistrationStatusEnum.COMPLETED);

        when(registrationRepository.findAllByUserIdsAndEvent(any(), eq(eventId))).thenReturn(List.of(reg1));

        assertThrows(EventAttendanceException.class, () ->
                registrationCommandService.fanpageMemberBatchAbsent(eventId, studentUserIds));
    }

    @Test
    @DisplayName("Should throw EventAttendanceException when requester is not active member of fanpage")
    void fanpageMemberBatchAbsent_notMember_shouldThrow() {
        Long eventId = 10L;
        Long fanpageId = 20L;
        Long memberId = 1L;
        List<Long> studentUserIds = List.of(101L);

        Event event = createTestEvent(eventId, fanpageId);
        when(securityContextPort.getCurrentUserId()).thenReturn(memberId);
        when(eventRepository.findById(eventId)).thenReturn(event);
        when(fanpageMemberRepository.isActiveMember(fanpageId, memberId)).thenReturn(false);

        assertThrows(EventAttendanceException.class, () ->
                registrationCommandService.fanpageMemberBatchAbsent(eventId, studentUserIds));
    }

    @Test
    @DisplayName("Should throw EventRegistrationException when event is inactive")
    void registerEvent_throwsException_whenEventIsInactive() {
        Long eventId = 10L;
        Event event = createTestEvent(eventId, 20L);
        event.setActive(false);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));

        when(eventRepository.findById(eventId)).thenReturn(event);

        assertThrows(EventRegistrationException.class, () ->
                registrationCommandService.registerEvent(eventId));
    }

    @Test
    @DisplayName("Should successfully register when event has no school restriction (schoolId is null)")
    void registerEvent_success_whenEventHasNoSchoolRestriction() {
        Long eventId = 10L;
        Long userId = 100L;

        Event event = createTestEvent(eventId, 20L);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setSchoolId(null);

        User user = new User();
        user.setId(userId);

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(registrationRepository.isRegistered(userId, eventId)).thenReturn(false);

        assertDoesNotThrow(() -> registrationCommandService.registerEvent(eventId));
        verify(registrationDtoMapper).updateRegistrationForRegister(any(Registration.class), eq(userId), eq(eventId), any(Instant.class), eq(RegistrationStatusEnum.REGISTERED));
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should successfully register when student belongs to the designated school")
    void registerEvent_success_whenStudentBelongsToDesignatedSchool() {
        Long eventId = 10L;
        Long userId = 100L;
        Long schoolId = 1L;

        Event event = createTestEvent(eventId, 20L);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setSchoolId(schoolId);

        User user = new User();
        user.setId(userId);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setSchoolId(schoolId);

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(registrationRepository.isRegistered(userId, eventId)).thenReturn(false);

        assertDoesNotThrow(() -> registrationCommandService.registerEvent(eventId));
        verify(registrationDtoMapper).updateRegistrationForRegister(any(Registration.class), eq(userId), eq(eventId), any(Instant.class), eq(RegistrationStatusEnum.REGISTERED));
        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should throw EventRegistrationException when student has no profile and event has school restriction")
    void registerEvent_throwsException_whenStudentHasNoProfile() {
        Long eventId = 10L;
        Long userId = 100L;
        Long schoolId = 1L;

        Event event = createTestEvent(eventId, 20L);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setSchoolId(schoolId);

        User user = new User();
        user.setId(userId);

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        EventRegistrationException ex = assertThrows(EventRegistrationException.class, () ->
                registrationCommandService.registerEvent(eventId));
        assertTrue(ex.getMessage().contains("Student profile not found"));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should throw EventRegistrationException when student profile has no school assigned")
    void registerEvent_throwsException_whenStudentHasNoSchool() {
        Long eventId = 10L;
        Long userId = 100L;
        Long schoolId = 1L;

        Event event = createTestEvent(eventId, 20L);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setSchoolId(schoolId);

        User user = new User();
        user.setId(userId);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setSchoolId(null);

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        EventRegistrationException ex = assertThrows(EventRegistrationException.class, () ->
                registrationCommandService.registerEvent(eventId));
        assertTrue(ex.getMessage().contains("missing major or school information"));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("Should throw EventRegistrationException when student belongs to a different school")
    void registerEvent_throwsException_whenStudentBelongsToDifferentSchool() {
        Long eventId = 10L;
        Long userId = 100L;
        Long eventSchoolId = 1L;
        Long studentSchoolId = 2L;

        Event event = createTestEvent(eventId, 20L);
        event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));
        event.setSchoolId(eventSchoolId);

        User user = new User();
        user.setId(userId);

        StudentProfile profile = new StudentProfile();
        profile.setUserId(userId);
        profile.setSchoolId(studentSchoolId);

        when(eventRepository.findById(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        EventRegistrationException ex = assertThrows(EventRegistrationException.class, () ->
                registrationCommandService.registerEvent(eventId));
        assertTrue(ex.getMessage().contains("only open to students from the designated school"));
        verify(registrationRepository, never()).save(any(Registration.class));
    }
}
