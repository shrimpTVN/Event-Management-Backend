package com.ddd.application.service.registration.impl;

import com.ddd.application.service.event.cache.RecommendationCacheService;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.domain.event.EventCheckoutAwardedEvent;
import com.ddd.domain.exception.BusinessException;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.ddd.domain.exception.EventFullException;
import com.ddd.domain.exception.EventGenderFullException;
import com.ddd.domain.exception.DuplicateResourceException;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
    private SecurityContextPort securityContextPort;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Spy
    private RegistrationDtoMapper registrationDtoMapper =
            Mappers.getMapper(RegistrationDtoMapper.class);

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
        event.setCapacity(100);
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
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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
        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);

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

        StudentProfile profile = new StudentProfile("SE12345", 19, 1L, userId);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
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

        StudentProfile profile = new StudentProfile("SE12345", 19, 1L, schoolId, userId);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(registrationRepository.isRegistered(userId, eventId)).thenReturn(false);

        assertDoesNotThrow(() -> registrationCommandService.registerEvent(eventId));
        verify(registrationDtoMapper).updateRegistrationForRegister(any(Registration.class), eq(userId), eq(eventId), any(Instant.class), eq(RegistrationStatusEnum.REGISTERED));
        verify(registrationRepository).save(any(Registration.class));
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

        StudentProfile profile = new StudentProfile("SE12345", 19, 1L, null, userId);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
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

        StudentProfile profile = new StudentProfile("SE12345", 19, 1L, studentSchoolId, userId);

        when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        EventRegistrationException ex = assertThrows(EventRegistrationException.class, () ->
                registrationCommandService.registerEvent(eventId));
        assertTrue(ex.getMessage().contains("only open to students from the designated school"));
        verify(registrationRepository, never()).save(any(Registration.class));
    }


    private Event eligibleStudent(int capacity) {
        Event event = createTestEvent(10L, 20L);
        event.setCapacity(capacity);
        User user = new User();
        user.setId(101L);
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(101L);
        when(userRepository.findById(101L)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(101L))
                .thenReturn(Optional.of(new StudentProfile("SE12345", 19, 1L, 101L)));
        return event;
    }

    private Registration existingRegistration(RegistrationStatusEnum status) {
        Registration registration = new Registration();
        registration.setStatus(status);
        registration.setUserId(101L);
        return registration;
    }

    @ParameterizedTest
    @CsvSource({"100,100,false", "100,101,false", "0,0,false", "100,100,true"})
    void rejectsFullEvent(int capacity, long occupied, boolean returningStudent) {
        Event event = eligibleStudent(capacity);
        if (returningStudent) {
            when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
            when(registrationRepository.findByUserAndEvent(101L, 10L))
                    .thenReturn(existingRegistration(RegistrationStatusEnum.CANCELLED));
        }
        when(registrationRepository.countActiveRegistrationsByEventId(10L)).thenReturn(occupied);
        assertThrows(EventFullException.class, () -> registrationCommandService.registerEvent(10L));
        verify(registrationRepository, never()).save(any());
        assertEquals(capacity, event.getCapacity());
    }

    @Test
    void reusesCancelledRowForFinalPlace() {
        Event event = eligibleStudent(100);
        Registration cancelled = existingRegistration(RegistrationStatusEnum.CANCELLED);
        cancelled.setReason("Schedule conflict");
        when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
        when(registrationRepository.findByUserAndEvent(101L, 10L)).thenReturn(cancelled);
        when(registrationRepository.countActiveRegistrationsByEventId(10L)).thenReturn(99L);
        registrationCommandService.registerEvent(10L);
        verify(registrationRepository).save(cancelled);
        assertNull(cancelled.getReason());
        assertEquals(RegistrationStatusEnum.REGISTERED, cancelled.getStatus());
        verify(recommendationCacheService).evictAfterCommit(101L);
        assertEquals(100, event.getCapacity());
    }

    private Event genderConstrainedEvent(int capacity, int maleQty, int femaleQty, String userGender) {
        Event event = createTestEvent(10L, 20L);
        event.setCapacity(capacity);
        event.setMaleQuantity(maleQty);
        event.setFemaleQuantity(femaleQty);
        event.setGenderConstraint(true);
        User user = new User();
        user.setId(101L);
        user.setGender(userGender);
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(101L);
        when(userRepository.findById(101L)).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(101L))
                .thenReturn(Optional.of(new StudentProfile("SE12345", 19, 1L, 101L)));
        return event;
    }

    @Test
    void genderConstraintDisabledIgnoresGenderAndQuotas() {
        Event event = eligibleStudent(10);
        event.setGenderConstraint(false);
        event.setMaleQuantity(0);
        event.setFemaleQuantity(0);
        when(registrationRepository.countActiveRegistrationsByEventId(10L)).thenReturn(0L);
        registrationCommandService.registerEvent(10L);
        verify(registrationRepository, never()).countActiveRegistrationsByEventIdAndGender(any(), any());
        verify(registrationRepository).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"OTHER", "other"})
    void genderConstraintRejectsNullOrOtherGender(String gender) {
        genderConstrainedEvent(10, 5, 5, gender);
        var ex = assertThrows(EventRegistrationException.class, () -> registrationCommandService.registerEvent(10L));
        assertFalse(ex instanceof EventGenderFullException);
        verify(registrationRepository, never()).save(any());
        verify(registrationRepository, never()).countActiveRegistrationsByEventIdAndGender(any(), any());
    }

    @Test
    void genderConstraintChecksDuplicateBeforeGenderQuota() {
        genderConstrainedEvent(10, 1, 1, "MALE");
        when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
        when(registrationRepository.findByUserAndEvent(101L, 10L))
                .thenReturn(existingRegistration(RegistrationStatusEnum.REGISTERED));
        assertThrows(DuplicateResourceException.class, () -> registrationCommandService.registerEvent(10L));
        verify(registrationRepository, never()).countActiveRegistrationsByEventIdAndGender(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"MALE,1,1,5", "MALE,0,0,5", "FEMALE,2,2,5", "FEMALE,0,0,5"})
    void genderConstraintRejectsWhenGenderQuotaFullEvenIfTotalCapacityAvailable(
            String gender, int quota, long occupiedForGender, int totalCapacity) {
        int maleQty = "MALE".equals(gender) ? quota : 3;
        int femaleQty = "FEMALE".equals(gender) ? quota : 3;
        genderConstrainedEvent(totalCapacity, maleQty, femaleQty, gender);
        when(registrationRepository.countActiveRegistrationsByEventIdAndGender(10L, gender))
                .thenReturn(occupiedForGender);
        assertThrows(EventGenderFullException.class, () -> registrationCommandService.registerEvent(10L));
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void genderConstraintRejectsCancelledReRegistrationWhenGenderQuotaFull() {
        genderConstrainedEvent(10, 1, 5, "MALE");
        Registration cancelled = existingRegistration(RegistrationStatusEnum.CANCELLED);
        when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
        when(registrationRepository.findByUserAndEvent(101L, 10L)).thenReturn(cancelled);
        when(registrationRepository.countActiveRegistrationsByEventIdAndGender(10L, "MALE")).thenReturn(1L);

        assertThrows(EventGenderFullException.class, () -> registrationCommandService.registerEvent(10L));
        assertEquals(RegistrationStatusEnum.CANCELLED, cancelled.getStatus());
        verify(registrationRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void genderConstraintAllowsRegistrationWithinQuotaIncludingCancelledReuse(boolean returningCancelled) {
        genderConstrainedEvent(10, 2, 2, "female");
        if (returningCancelled) {
            Registration cancelled = existingRegistration(RegistrationStatusEnum.CANCELLED);
            when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
            when(registrationRepository.findByUserAndEvent(101L, 10L)).thenReturn(cancelled);
        }
        when(registrationRepository.countActiveRegistrationsByEventIdAndGender(10L, "FEMALE")).thenReturn(1L);
        when(registrationRepository.countActiveRegistrationsByEventId(10L)).thenReturn(3L);
        registrationCommandService.registerEvent(10L);
        verify(registrationRepository).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RegistrationStatusEnum.class, names = "CANCELLED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsDuplicateBeforeCounting(RegistrationStatusEnum status) {
        eligibleStudent(0);
        when(registrationRepository.isRegistered(101L, 10L)).thenReturn(true);
        when(registrationRepository.findByUserAndEvent(101L, 10L)).thenReturn(existingRegistration(status));
        assertThrows(DuplicateResourceException.class, () -> registrationCommandService.registerEvent(10L));
        verify(registrationRepository, never()).countActiveRegistrationsByEventId(any());
    }

    @ParameterizedTest
    @EnumSource(value = RegistrationStatusEnum.class, names = {"CANCELLED", "RECORDED"})
    void preservesCancelledAndRecordedRowsDuringAbsenceAndCheckout(RegistrationStatusEnum status) {
        Event event = createTestEvent(10L, 20L);
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(1L);
        when(fanpageMemberRepository.isActiveMember(20L, 1L)).thenReturn(true);
        Registration protectedRow = existingRegistration(status);
        when(registrationRepository.findAllByUserIdsAndEvent(List.of(101L), 10L)).thenReturn(List.of(protectedRow));
        assertThrows(EventAttendanceException.class, () -> registrationCommandService.fanpageMemberBatchAbsent(10L, List.of(101L)));
        assertThrows(EventAttendanceException.class, () -> registrationCommandService.fanpageMemberCheckOut(
                10L, List.of(101L), RegistrationStatusEnum.COMPLETED));
        assertEquals(status, protectedRow.getStatus());
        verify(registrationRepository, never()).saveAll(any());
    }

    @Test
    void completionPreservesRecordedAndCancelledAndOnlyRunsOnce() {
        Event event = createTestEvent(10L, 20L);
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(1L);
        when(fanpageMemberRepository.isActiveMember(20L, 1L)).thenReturn(true);
        Registration cancelled = existingRegistration(RegistrationStatusEnum.CANCELLED);
        Registration recorded = existingRegistration(RegistrationStatusEnum.RECORDED);
        recorded.setUserId(102L);
        Registration confirmed = existingRegistration(RegistrationStatusEnum.CONFIRMED);
        confirmed.setUserId(103L);
        when(registrationRepository.findAllByEvent(10L)).thenReturn(List.of(cancelled, recorded, confirmed));
        registrationCommandService.fanpageMemberMarkEventAsCompleted(10L);
        registrationCommandService.fanpageMemberMarkEventAsCompleted(10L);
        assertEquals(RegistrationStatusEnum.CANCELLED, cancelled.getStatus());
        assertEquals(RegistrationStatusEnum.RECORDED, recorded.getStatus());
        assertEquals(RegistrationStatusEnum.RECORDED, confirmed.getStatus());
        assertEquals(EventStatusEnum.COMPLETED.name(), event.getStatus());
        verify(eventRepository).save(event);
        verify(registrationRepository).findAllByEvent(10L);
        verify(applicationEventPublisher).publishEvent(new EventCheckoutAwardedEvent(
                10L, event.getName(), event.getBannerUrl(), List.of(103L)));
    }

    @Test
    void cancellationReleasesPlaceOnlyOnce() {
        when(securityContextPort.getCurrentUserId()).thenReturn(101L);
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(createTestEvent(10L, 20L));
        Registration registered = existingRegistration(RegistrationStatusEnum.REGISTERED);
        when(registrationRepository.findByUserAndEvent(101L, 10L)).thenReturn(registered);
        registrationCommandService.cancelRegistration(10L, "Schedule conflict");
        assertEquals(RegistrationStatusEnum.CANCELLED, registered.getStatus());
        assertThrows(EventRegistrationException.class, () -> registrationCommandService.cancelRegistration(10L, "Again"));
        verify(registrationRepository).save(registered);
        verify(recommendationCacheService).evictAfterCommit(101L);
    }

    @Test
    void newStudentCanTakeTheFinalPlace() {
        Event event = eligibleStudent(100);
        when(registrationRepository.countActiveRegistrationsByEventId(10L)).thenReturn(99L);
        registrationCommandService.registerEvent(10L);
        verify(registrationRepository).save(any());
        assertEquals(100, event.getCapacity());
    }

    @Test
    void missingUserCannotRegister() {
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(createTestEvent(10L, 20L));
        when(securityContextPort.getCurrentUserId()).thenReturn(101L);
        var ex = assertThrows(BusinessException.class,
                () -> registrationCommandService.registerEvent(10L));
        assertEquals("User not found", ex.getMessage());
        verifyNoInteractions(registrationRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed", "future", "draft"})
    void registrationRequiresPublishedEventInsideRegistrationWindow(String scenario) {
        Event event = createTestEvent(10L, 20L);
        switch (scenario) {
            case "closed" -> event.setDateClose(Instant.now().minusSeconds(60));
            case "future" -> event.setDateOpen(Instant.now().plusSeconds(60));
            case "draft" -> event.setStatus(EventStatusEnum.DRAFT.name());
        }
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        assertThrows(EventRegistrationException.class, () -> registrationCommandService.registerEvent(10L));
        verifyNoInteractions(registrationRepository);
    }

    @Test
    void completedAttendanceCannotBeMarkedAbsent() {
        Event event = createTestEvent(10L, 20L);
        event.setStatus(EventStatusEnum.COMPLETED.name());
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(event);
        when(securityContextPort.getCurrentUserId()).thenReturn(1L);
        when(fanpageMemberRepository.isActiveMember(20L, 1L)).thenReturn(true);
        assertThrows(EventAttendanceException.class,
                () -> registrationCommandService.fanpageMemberBatchAbsent(10L, List.of(101L)));
        verifyNoInteractions(registrationRepository);
    }

    @Nested
    @DisplayName("Event Registration StudentProfile Gating Tests")
    class RegisterEventGateTests {

        @Test
        @DisplayName("shouldThrowStudentProfileRequiredExceptionWhenProfileDoesNotExist")
        void shouldThrowStudentProfileRequiredExceptionWhenProfileDoesNotExist() {
            Long eventId = 10L;
            Event event = createTestEvent(eventId, 20L);
            event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
            event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));

            User user = new User();
            user.setId(101L);

            when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
            when(securityContextPort.getCurrentUserId()).thenReturn(101L);
            when(userRepository.findById(101L)).thenReturn(Optional.of(user));
            when(studentProfileRepository.findByUserId(101L)).thenReturn(Optional.empty());

            assertThrows(StudentProfileRequiredException.class, () ->
                    registrationCommandService.registerEvent(eventId));
            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldThrowStudentProfileRequiredExceptionWhenProfileIsIncomplete")
        void shouldThrowStudentProfileRequiredExceptionWhenProfileIsIncomplete() {
            Long eventId = 10L;
            Event event = createTestEvent(eventId, 20L);
            event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
            event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));

            User user = new User();
            user.setId(101L);

            StudentProfile incompleteProfile = new StudentProfile(null, 19, 1L, 101L);

            when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
            when(securityContextPort.getCurrentUserId()).thenReturn(101L);
            when(userRepository.findById(101L)).thenReturn(Optional.of(user));
            when(studentProfileRepository.findByUserId(101L)).thenReturn(Optional.of(incompleteProfile));

            assertThrows(StudentProfileRequiredException.class, () ->
                    registrationCommandService.registerEvent(eventId));
            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("shouldProceedWithRegistrationWhenStudentProfileIsComplete")
        void shouldProceedWithRegistrationWhenStudentProfileIsComplete() {
            Long eventId = 10L;
            Event event = createTestEvent(eventId, 20L);
            event.setDateOpen(Instant.now().minus(1, ChronoUnit.DAYS));
            event.setDateClose(Instant.now().plus(1, ChronoUnit.DAYS));

            User user = new User();
            user.setId(101L);

            StudentProfile completeProfile = new StudentProfile("SE12345", 19, 1L, 101L);

            when(eventRepository.findByIdForUpdate(eventId)).thenReturn(event);
            when(securityContextPort.getCurrentUserId()).thenReturn(101L);
            when(userRepository.findById(101L)).thenReturn(Optional.of(user));
            when(studentProfileRepository.findByUserId(101L)).thenReturn(Optional.of(completeProfile));
            when(registrationRepository.isRegistered(101L, eventId)).thenReturn(false);

            assertDoesNotThrow(() -> registrationCommandService.registerEvent(eventId));
            verify(registrationRepository).save(any(Registration.class));
        }
    }
}
