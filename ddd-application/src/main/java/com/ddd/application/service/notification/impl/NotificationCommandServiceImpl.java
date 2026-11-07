package com.ddd.application.service.notification.impl;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.mapper.NotificationDtoMapper;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationPushPort notificationPushPort;
    private final NotificationDtoMapper notificationDtoMapper;
    private final SecurityContextPort securityContextPort;

    @Override
    public NotificationInfoDto createNotification(CreateNotificationDto dto) {
        log.info("Creating notification for user: {}", dto.userId());

        Notification notification = new Notification();
        notification.setTitle(dto.title());
        notification.setMessage(dto.message());
        notification.setStatus(NotificationStatusEnum.UNREAD);
        notification.setIconUrl(dto.iconUrl());
        notification.setUrl(dto.url());
        notification.setUserId(dto.userId());

        Notification saved = notificationRepository.save(notification);

        try {
            notificationPushPort.pushToUser(dto.userId(), saved);
        } catch (Exception e) {
            log.error("Failed to push real-time notification to user {}: {}", dto.userId(), e.getMessage());
        }

        return notificationDtoMapper.toInfoDto(saved);
    }

    @Override
    public void markAsRead(Long notificationId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!currentUserId.equals(notification.getUserId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notificationRepository.markAsRead(notificationId, currentUserId);
    }

    @Override
    public void markAllAsRead() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        notificationRepository.markAllAsRead(currentUserId);
    }

    @Override
    public void deleteNotification(Long notificationId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

        if (!currentUserId.equals(notification.getUserId())) {
            throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
        }

        notificationRepository.deleteByIdAndUserId(notificationId, currentUserId);
    }
}
