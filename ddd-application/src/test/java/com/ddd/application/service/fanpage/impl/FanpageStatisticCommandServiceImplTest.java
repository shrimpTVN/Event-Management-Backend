package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.statistic.AlertAbsentStudentsDto;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.domain.enums.NotificationTypeEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanpageStatisticCommandServiceImplTest {

    @Mock
    private FanpageRepository fanpageRepository;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private NotificationCommandService notificationCommandService;

    @InjectMocks
    private FanpageStatisticCommandServiceImpl commandService;

    private final Long fanpageId = 1L;
    private final Long currentUserId = 100L;

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fanpage does not exist")
    void shouldThrowWhenFanpageNotFound() {
        when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.empty());

        AlertAbsentStudentsDto dto = new AlertAbsentStudentsDto(5L, List.of(42L), "Warning message");

        assertThrows(ResourceNotFoundException.class, () ->
                commandService.alertAbsentStudents(fanpageId, dto));
        verifyNoInteractions(notificationCommandService);
    }

    @Test
    @DisplayName("Should throw AuthException when caller is not an active member")
    void shouldThrowWhenCallerNotActiveMember() {
        Fanpage fanpage = new Fanpage();
        fanpage.setId(fanpageId);
        when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(fanpage));
        when(securityContextPort.getCurrentUserId()).thenReturn(currentUserId);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(false);
        when(fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)).thenReturn(false);

        AlertAbsentStudentsDto dto = new AlertAbsentStudentsDto(5L, List.of(42L), "Warning message");

        assertThrows(AuthException.class, () ->
                commandService.alertAbsentStudents(fanpageId, dto));
        verifyNoInteractions(notificationCommandService);
    }

    @Test
    @DisplayName("Should successfully dispatch notifications to all students")
    void shouldDispatchNotificationsSuccessfully() {
        Fanpage fanpage = new Fanpage();
        fanpage.setId(fanpageId);
        fanpage.setName("IT Club");
        fanpage.setAvatarUrl("https://avatar.png");

        when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(fanpage));
        when(securityContextPort.getCurrentUserId()).thenReturn(currentUserId);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

        AlertAbsentStudentsDto dto = new AlertAbsentStudentsDto(
                5L,
                List.of(42L, 43L),
                "Custom warning message"
        );

        assertDoesNotThrow(() -> commandService.alertAbsentStudents(fanpageId, dto));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateNotificationDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationCommandService).createNotifications(captor.capture());

        List<CreateNotificationDto> dispatched = captor.getValue();
        assertEquals(2, dispatched.size());
        assertEquals(42L, dispatched.get(0).userId());
        assertEquals(NotificationTypeEnum.EVENT_ABSENCE_ALERT, dispatched.get(0).type());
        assertEquals("Custom warning message", dispatched.get(0).message());
        assertEquals("https://avatar.png", dispatched.get(0).iconUrl());
        assertEquals("/fanpages/1", dispatched.get(0).url());
        assertEquals(43L, dispatched.get(1).userId());
    }

    @Test
    @DisplayName("Should use default message when custom message is null or blank")
    void shouldUseDefaultMessageWhenCustomMessageIsBlank() {
        Fanpage fanpage = new Fanpage();
        fanpage.setId(fanpageId);
        fanpage.setName("IT Club");

        when(fanpageRepository.findByIdAndIsActive(fanpageId, true)).thenReturn(Optional.of(fanpage));
        when(securityContextPort.getCurrentUserId()).thenReturn(currentUserId);
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);

        AlertAbsentStudentsDto dto = new AlertAbsentStudentsDto(5L, List.of(42L), "   ");

        assertDoesNotThrow(() -> commandService.alertAbsentStudents(fanpageId, dto));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CreateNotificationDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationCommandService).createNotifications(captor.capture());

        List<CreateNotificationDto> dispatched = captor.getValue();
        assertEquals(1, dispatched.size());
        assertTrue(dispatched.get(0).message().contains("Warning from IT Club"));
    }
}
