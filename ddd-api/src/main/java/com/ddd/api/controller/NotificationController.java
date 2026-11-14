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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationCommandService notificationCommandService;
    private final NotificationQueryService notificationQueryService;
    private final NotificationRealtimeService notificationRealtimeService;
    private final NotificationApiMapper notificationApiMapper;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications() {
        return notificationRealtimeService.subscribe();
    }

    @GetMapping
    public BaseResponse<Page<NotificationResponseDto>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) NotificationStatusEnum status) {
        Page<NotificationInfoDto> result = notificationQueryService.getMyNotifications(page, size, sortBy, sortDir, status);
        return BaseResponse.of(result.map(notificationApiMapper::toResponseDto));
    }

    @GetMapping("/unread-count")
    public BaseResponse<UnreadCountResponseDto> getUnreadCount() {
        long count = notificationQueryService.getMyUnreadCount();
        return BaseResponse.of(new UnreadCountResponseDto(count));
    }

    @PatchMapping("/{id}/read")
    public BaseResponse<Void> markAsRead(@PathVariable Long id) {
        notificationCommandService.markAsRead(id);
        return BaseResponse.ok();
    }

    @PatchMapping("/read-all")
    public BaseResponse<Void> markAllAsRead() {
        notificationCommandService.markAllAsRead();
        return BaseResponse.ok();
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> deleteNotification(@PathVariable Long id) {
        notificationCommandService.deleteNotification(id);
        return BaseResponse.ok();
    }

    @GetMapping("/fanpages/{fanpageId}")
    public BaseResponse<Page<NotificationResponseDto>> getFanpageNotifications(
            @PathVariable Long fanpageId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) NotificationStatusEnum status) {
        Page<NotificationInfoDto> result = notificationQueryService.getFanpageNotifications(fanpageId, page, size, sortBy, sortDir, status);
        return BaseResponse.of(result.map(notificationApiMapper::toResponseDto));
    }

    @GetMapping("/fanpages/{fanpageId}/unread-count")
    public BaseResponse<UnreadCountResponseDto> getFanpageUnreadCount(@PathVariable Long fanpageId) {
        long count = notificationQueryService.getFanpageUnreadCount(fanpageId);
        return BaseResponse.of(new UnreadCountResponseDto(count));
    }

    @PatchMapping("/fanpages/{fanpageId}/{id}/read")
    public BaseResponse<Void> markAsReadByFanpage(
            @PathVariable Long fanpageId,
            @PathVariable Long id) {
        notificationCommandService.markAsReadByFanpage(id, fanpageId);
        return BaseResponse.ok();
    }

    @PatchMapping("/fanpages/{fanpageId}/read-all")
    public BaseResponse<Void> markAllAsReadByFanpage(@PathVariable Long fanpageId) {
        notificationCommandService.markAllAsReadByFanpage(fanpageId);
        return BaseResponse.ok();
    }

    @DeleteMapping("/fanpages/{fanpageId}/{id}")
    public BaseResponse<Void> deleteFanpageNotification(
            @PathVariable Long fanpageId,
            @PathVariable Long id) {
        notificationCommandService.deleteFanpageNotification(id, fanpageId);
        return BaseResponse.ok();
    }
}
