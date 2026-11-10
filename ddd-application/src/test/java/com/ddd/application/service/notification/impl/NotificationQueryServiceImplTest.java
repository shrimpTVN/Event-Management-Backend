package com.ddd.application.service.notification.impl;

import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.mapper.NotificationDtoMapper;
import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDtoMapper notificationDtoMapper;

    @Mock
    private SecurityContextPort securityContextPort;

    @Mock
    private FanpageMemberRepository fanpageMemberRepository;

    @InjectMocks
    private NotificationQueryServiceImpl notificationQueryService;

    @Test
    @DisplayName("Should return paginated notifications without status filter")
    void getMyNotifications_withoutStatus() {
        Long userId = 1L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        Notification notification = new Notification("Title", "Message", NotificationStatusEnum.UNREAD, null, null, userId);
        notification.setId(10L);
        Page<Notification> domainPage = new PageImpl<>(List.of(notification));

        NotificationInfoDto infoDto = new NotificationInfoDto(10L, "Title", "Message", NotificationStatusEnum.UNREAD, null, null, userId, Instant.now());

        when(notificationRepository.findAllByUserId(eq(userId), any(Pageable.class))).thenReturn(domainPage);
        when(notificationDtoMapper.toInfoDto(notification)).thenReturn(infoDto);

        Page<NotificationInfoDto> result = notificationQueryService.getMyNotifications(0, 10, "createdAt", "desc", null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).id());
        verify(notificationRepository).findAllByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return paginated notifications with status filter")
    void getMyNotifications_withStatus() {
        Long userId = 1L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);

        Notification notification = new Notification("Title", "Message", NotificationStatusEnum.UNREAD, null, null, userId);
        notification.setId(10L);
        Page<Notification> domainPage = new PageImpl<>(List.of(notification));

        NotificationInfoDto infoDto = new NotificationInfoDto(10L, "Title", "Message", NotificationStatusEnum.UNREAD, null, null, userId, Instant.now());

        when(notificationRepository.findAllByUserIdAndStatus(eq(userId), eq(NotificationStatusEnum.UNREAD), any(Pageable.class))).thenReturn(domainPage);
        when(notificationDtoMapper.toInfoDto(notification)).thenReturn(infoDto);

        Page<NotificationInfoDto> result = notificationQueryService.getMyNotifications(0, 10, "createdAt", "desc", NotificationStatusEnum.UNREAD);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findAllByUserIdAndStatus(eq(userId), eq(NotificationStatusEnum.UNREAD), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return unread count for current user")
    void getMyUnreadCount_success() {
        Long userId = 1L;
        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(5L);

        long count = notificationQueryService.getMyUnreadCount();

        assertEquals(5L, count);
        verify(notificationRepository).countUnreadByUserId(userId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when unauthenticated")
    void getMyUnreadCount_unauthenticated() {
        when(securityContextPort.getCurrentUserId()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> notificationQueryService.getMyUnreadCount());
    }

    @Test
    @DisplayName("Should return paginated fanpage notifications when user is admin")
    void getFanpageNotifications_asAdmin_success() {
        Long fanpageId = 100L;
        Long userId = 1L;

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageMemberRepository.isAdminOfFanpage(fanpageId, userId)).thenReturn(true);

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setFanpageId(fanpageId);
        Page<Notification> domainPage = new PageImpl<>(List.of(notification));

        NotificationInfoDto infoDto = new NotificationInfoDto(10L, "Fanpage Title", "Message",
                NotificationStatusEnum.UNREAD, null, null, null, fanpageId, Instant.now());

        when(notificationRepository.findAllByFanpageId(eq(fanpageId), any(Pageable.class))).thenReturn(domainPage);
        when(notificationDtoMapper.toInfoDto(notification)).thenReturn(infoDto);

        Page<NotificationInfoDto> result = notificationQueryService.getFanpageNotifications(fanpageId, 0, 10, "createdAt", "desc", null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(notificationRepository).findAllByFanpageId(eq(fanpageId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should throw AuthException when querying fanpage notifications and user is not admin")
    void getFanpageNotifications_notAdmin_shouldThrow() {
        Long fanpageId = 100L;
        Long userId = 1L;

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageMemberRepository.isAdminOfFanpage(fanpageId, userId)).thenReturn(false);

        assertThrows(AuthException.class, () -> notificationQueryService.getFanpageNotifications(fanpageId, 0, 10, "createdAt", "desc", null));
        verify(notificationRepository, never()).findAllByFanpageId(any(), any());
    }

    @Test
    @DisplayName("Should return fanpage unread count when user is admin")
    void getFanpageUnreadCount_asAdmin_success() {
        Long fanpageId = 100L;
        Long userId = 1L;

        when(securityContextPort.getCurrentUserId()).thenReturn(userId);
        when(fanpageMemberRepository.isAdminOfFanpage(fanpageId, userId)).thenReturn(true);
        when(notificationRepository.countUnreadByFanpageId(fanpageId)).thenReturn(3L);

        long count = notificationQueryService.getFanpageUnreadCount(fanpageId);

        assertEquals(3L, count);
        verify(notificationRepository).countUnreadByFanpageId(fanpageId);
    }
}
