package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.model.Notification;
import com.ddd.domain.repository.NotificationRepository;
import com.ddd.infrastructure.entity.BaseEntityJpa;
import com.ddd.infrastructure.entity.NotificationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.NotificationMapper;
import com.ddd.infrastructure.repository.jpaRepository.NotificationJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;
    private final EntityManager entityManager;

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = notificationMapper.toJpaEntity(notification);
        if (notification.getId() != null) {
            entity.setId(notification.getId());
        }

        if (notification.getUserId() != null) {
            UserJpaEntity userRef = entityManager.getReference(UserJpaEntity.class, notification.getUserId());
            entity.setUser(userRef);
        }

        NotificationJpaEntity saved = notificationJpaRepository.save(entity);
        return notificationMapper.toDomainModel(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return notificationJpaRepository.findById(id)
                .filter(entity -> Boolean.TRUE.equals(entity.getIsActive()))
                .map(notificationMapper::toDomainModel);
    }

    @Override
    public Page<Notification> findAllByUserId(Long userId, Pageable pageable) {
        return notificationJpaRepository.findByUser_IdAndIsActiveTrue(userId, pageable)
                .map(notificationMapper::toDomainModel);
    }

    @Override
    public Page<Notification> findAllByUserIdAndStatus(Long userId, NotificationStatusEnum status, Pageable pageable) {
        return notificationJpaRepository.findByUser_IdAndStatusAndIsActiveTrue(userId, status, pageable)
                .map(notificationMapper::toDomainModel);
    }

    @Override
    public long countUnreadByUserId(Long userId) {
        return notificationJpaRepository.countByUser_IdAndStatusAndIsActiveTrue(userId, NotificationStatusEnum.UNREAD);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        notificationJpaRepository.updateStatusByIdAndUserId(id, userId, NotificationStatusEnum.READ);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationJpaRepository.updateAllStatusByUserId(userId, NotificationStatusEnum.UNREAD, NotificationStatusEnum.READ);
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        notificationJpaRepository.softDeleteByIdAndUserId(id, userId);
    }
}
