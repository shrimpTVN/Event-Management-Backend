package com.ddd.application.service.notification.impl;

import com.ddd.application.service.notification.NotificationRealtimeService;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.adapter.notification.SseNotificationPushAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationRealtimeServiceImpl implements NotificationRealtimeService {

    private final SseNotificationPushAdapter sseNotificationPushAdapter;
    private final SecurityContextPort securityContextPort;

    @Override
    public SseEmitter subscribe() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        log.info("Client subscribed to notification stream: userId={}", currentUserId);
        return sseNotificationPushAdapter.createEmitter(currentUserId);
    }
}
