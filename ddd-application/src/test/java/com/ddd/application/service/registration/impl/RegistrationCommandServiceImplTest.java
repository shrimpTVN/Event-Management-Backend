package com.ddd.application.service.registration.impl;

import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.Registration;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
}
