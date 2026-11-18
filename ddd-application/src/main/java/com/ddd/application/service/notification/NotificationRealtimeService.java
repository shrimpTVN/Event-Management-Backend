package com.ddd.application.service.notification;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface NotificationRealtimeService {
    SseEmitter subscribe();
}
