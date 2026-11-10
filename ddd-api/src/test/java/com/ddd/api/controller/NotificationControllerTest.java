package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.notification.res.NotificationResponseDto;
import com.ddd.api.dto.notification.res.UnreadCountResponseDto;
import com.ddd.api.mapper.NotificationApiMapper;
import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.application.service.notification.NotificationQueryService;
import com.ddd.application.service.notification.NotificationRealtimeService;
import com.ddd.domain.enums.NotificationStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private NotificationQueryService notificationQueryService;

    @Mock
    private NotificationRealtimeService notificationRealtimeService;

    @Mock
    private NotificationApiMapper notificationApiMapper;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    @DisplayName("streamNotifications should return SseEmitter from realtime service")
    void streamNotifications() {
        SseEmitter mockEmitter = new SseEmitter();
        when(notificationRealtimeService.subscribe()).thenReturn(mockEmitter);

        SseEmitter result = notificationController.streamNotifications();

        assertEquals(mockEmitter, result);
        verify(notificationRealtimeService).subscribe();
    }

    @Test
    @DisplayName("getMyNotifications should return mapped response page")
    void getMyNotifications() {
        NotificationInfoDto infoDto = new NotificationInfoDto(1L, "Title", "Msg", NotificationStatusEnum.UNREAD, null, null, 10L, Instant.now());
        NotificationResponseDto resDto = new NotificationResponseDto(1L, "Title", "Msg", NotificationStatusEnum.UNREAD, null, null, Instant.now());
        Page<NotificationInfoDto> page = new PageImpl<>(List.of(infoDto));

        when(notificationQueryService.getMyNotifications(0, 10, "createdAt", "desc", null)).thenReturn(page);
        when(notificationApiMapper.toResponseDto(infoDto)).thenReturn(resDto);

        BaseResponse<Page<NotificationResponseDto>> response = notificationController.getMyNotifications(0, 10, "createdAt", "desc", null);

        assertNotNull(response);
        assertEquals(1, response.data().getTotalElements());
        assertEquals("Title", response.data().getContent().get(0).title());
    }

    @Test
    @DisplayName("getUnreadCount should return unread count response")
    void getUnreadCount() {
        when(notificationQueryService.getMyUnreadCount()).thenReturn(7L);

        BaseResponse<UnreadCountResponseDto> response = notificationController.getUnreadCount();

        assertNotNull(response);
        assertEquals(7L, response.data().unreadCount());
    }

    @Test
    @DisplayName("markAsRead should delegate to command service")
    void markAsRead() {
        BaseResponse<Void> response = notificationController.markAsRead(10L);

        assertNotNull(response);
        verify(notificationCommandService).markAsRead(10L);
    }

    @Test
    @DisplayName("markAllAsRead should delegate to command service")
    void markAllAsRead() {
        BaseResponse<Void> response = notificationController.markAllAsRead();

        assertNotNull(response);
        verify(notificationCommandService).markAllAsRead();
    }

    @Test
    @DisplayName("deleteNotification should delegate to command service")
    void deleteNotification() {
        BaseResponse<Void> response = notificationController.deleteNotification(10L);

        assertNotNull(response);
        verify(notificationCommandService).deleteNotification(10L);
    }

    @Test
    @DisplayName("getFanpageNotifications should return mapped response page")
    void getFanpageNotifications() {
        Long fanpageId = 100L;
        NotificationInfoDto infoDto = new NotificationInfoDto(1L, "Title", "Msg", NotificationStatusEnum.UNREAD, null, null, null, fanpageId, Instant.now());
        NotificationResponseDto resDto = new NotificationResponseDto(1L, "Title", "Msg", NotificationStatusEnum.UNREAD, null, null, fanpageId, Instant.now());
        Page<NotificationInfoDto> page = new PageImpl<>(List.of(infoDto));

        when(notificationQueryService.getFanpageNotifications(fanpageId, 0, 10, "createdAt", "desc", null)).thenReturn(page);
        when(notificationApiMapper.toResponseDto(infoDto)).thenReturn(resDto);

        BaseResponse<Page<NotificationResponseDto>> response = notificationController.getFanpageNotifications(fanpageId, 0, 10, "createdAt", "desc", null);

        assertNotNull(response);
        assertEquals(1, response.data().getTotalElements());
        assertEquals(fanpageId, response.data().getContent().get(0).fanpageId());
    }

    @Test
    @DisplayName("getFanpageUnreadCount should return unread count for fanpage")
    void getFanpageUnreadCount() {
        Long fanpageId = 100L;
        when(notificationQueryService.getFanpageUnreadCount(fanpageId)).thenReturn(3L);

        BaseResponse<UnreadCountResponseDto> response = notificationController.getFanpageUnreadCount(fanpageId);

        assertNotNull(response);
        assertEquals(3L, response.data().unreadCount());
    }

    @Test
    @DisplayName("markAsReadByFanpage should delegate to command service")
    void markAsReadByFanpage() {
        BaseResponse<Void> response = notificationController.markAsReadByFanpage(100L, 10L);

        assertNotNull(response);
        verify(notificationCommandService).markAsReadByFanpage(10L, 100L);
    }

    @Test
    @DisplayName("markAllAsReadByFanpage should delegate to command service")
    void markAllAsReadByFanpage() {
        BaseResponse<Void> response = notificationController.markAllAsReadByFanpage(100L);

        assertNotNull(response);
        verify(notificationCommandService).markAllAsReadByFanpage(100L);
    }

    @Test
    @DisplayName("deleteFanpageNotification should delegate to command service")
    void deleteFanpageNotification() {
        BaseResponse<Void> response = notificationController.deleteFanpageNotification(100L, 10L);

        assertNotNull(response);
        verify(notificationCommandService).deleteFanpageNotification(10L, 100L);
    }
}
