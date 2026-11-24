package com.ddd.application.service.notification;

import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchAfterCommit {

    private final NotificationPushPort notificationPushPort;

    public void dispatchSingleAfterCommit(Notification notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Cannot schedule after-commit notification dispatch: no active transaction synchronization");
        }
        if (notification == null) {
            return;
        }

        Notification snapshot = createSnapshot(notification);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (snapshot.getUserId() != null) {
                    try {
                        notificationPushPort.pushToUser(snapshot.getUserId(), snapshot);
                    } catch (Exception e) {
                        log.error("Failed to push after-commit notification {} to user {}: {}",
                                snapshot.getId(), snapshot.getUserId(), e.getMessage());
                    }
                }

                if (snapshot.getFanpageId() != null) {
                    try {
                        notificationPushPort.pushToFanpage(snapshot.getFanpageId(), snapshot);
                    } catch (Exception e) {
                        log.error("Failed to push after-commit notification {} to fanpage {}: {}",
                                snapshot.getId(), snapshot.getFanpageId(), e.getMessage());
                    }
                }
            }
        });
    }

    public void dispatchAfterCommit(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Cannot schedule after-commit notification dispatch: no active transaction synchronization");
        }

        List<Notification> snapshots = notifications.stream()
                .map(this::createSnapshot)
                .toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Notification snapshot : snapshots) {
                    if (snapshot.getUserId() != null) {
                        try {
                            notificationPushPort.pushToUser(snapshot.getUserId(), snapshot);
                        } catch (Exception e) {
                            log.error("Failed to push after-commit batch notification {} to user {}: {}",
                                    snapshot.getId(), snapshot.getUserId(), e.getMessage());
                        }
                    } else if (snapshot.getFanpageId() != null) {
                        try {
                            notificationPushPort.pushToFanpage(snapshot.getFanpageId(), snapshot);
                        } catch (Exception e) {
                            log.error("Failed to push after-commit batch notification {} to fanpage {}: {}",
                                    snapshot.getId(), snapshot.getFanpageId(), e.getMessage());
                        }
                    }
                }
            }
        });
    }

    private Notification createSnapshot(Notification original) {
        if (original == null) {
            return null;
        }
        Notification copy = new Notification();
        copy.setId(original.getId());
        copy.setType(original.getType());
        copy.setMessage(original.getMessage());
        copy.setStatus(original.getStatus());
        copy.setIconUrl(original.getIconUrl());
        copy.setUrl(original.getUrl());
        copy.setUserId(original.getUserId());
        copy.setFanpageId(original.getFanpageId());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());
        copy.setCreatedBy(original.getCreatedBy());
        copy.setUpdatedBy(original.getUpdatedBy());
        copy.setActive(original.isActive());
        return copy;
    }
}
