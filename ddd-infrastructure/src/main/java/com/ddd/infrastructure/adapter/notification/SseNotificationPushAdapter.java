package com.ddd.infrastructure.adapter.notification;

import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseNotificationPushAdapter implements NotificationPushPort {

    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30 minutes
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;

    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE error for user {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("Connected to notification stream"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event to user {}", userId);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }
    @Override
    public void pushToUser(Long userId, Notification notification) {
        if (userId == null || notification == null) {
            return;
        }

        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        // Convert payload once to avoid repetitive Jackson reflection
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .id(notification.getId() != null ? String.valueOf(notification.getId()) : null)
                .name("NOTIFICATION")
                .data(notification);

        for (SseEmitter emitter : emitters) {
            CompletableFuture.runAsync(() -> {
                try {
                    emitter.send(event);
                } catch (Exception e) {
                    removeEmitter(userId, emitter);
                }
            });
        }
    }


    @Override
    public void pushToFanpage(Long fanpageId, Notification notification) {
        if (fanpageId == null || notification == null) {
            return;
        }

        List<Long> adminUserIds = fanpageMemberJpaRepository.findUserIdsByFanpageIdAndRole(fanpageId, FanpageRoleEnum.ADMIN.name());
        if (adminUserIds == null || adminUserIds.isEmpty()) {
            log.debug("No active admins found for fanpage {}", fanpageId);
            return;
        }

        for (Long adminUserId : adminUserIds) {
            pushToUser(adminUserId, notification);
        }
    }

    @Scheduled(fixedRate = 45000)
    public void sendHeartbeat() {
        if (userEmitters.isEmpty()) {
            return;
        }

        userEmitters.forEach((userId, emitters) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            if (!deadEmitters.isEmpty()) {
                emitters.removeAll(deadEmitters);
            }
        });
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }
}
