package com.ddd.domain.repository;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);

    Optional<Notification> findById(Long id);

    Page<Notification> findAllByUserId(Long userId, Pageable pageable);

    Page<Notification> findAllByUserIdAndStatus(Long userId, NotificationStatusEnum status, Pageable pageable);

    long countUnreadByUserId(Long userId);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    Page<Notification> findAllByFanpageId(Long fanpageId, Pageable pageable);

    Page<Notification> findAllByFanpageIdAndStatus(Long fanpageId, NotificationStatusEnum status, Pageable pageable);

    long countUnreadByFanpageId(Long fanpageId);

    void markAsReadByFanpage(Long id, Long fanpageId);

    void markAllAsReadByFanpage(Long fanpageId);

    void deleteByIdAndFanpageId(Long id, Long fanpageId);
}
