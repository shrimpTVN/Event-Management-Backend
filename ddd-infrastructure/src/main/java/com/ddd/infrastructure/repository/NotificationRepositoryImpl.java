package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.model.Notification;
import com.ddd.domain.repository.NotificationRepository;
import com.ddd.infrastructure.entity.BaseEntityJpa;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.NotificationJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.NotificationMapper;
import com.ddd.infrastructure.repository.jpaRepository.NotificationJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationMapper notificationMapper;
    private final EntityManager entityManager;

    private void assignReferences(Notification notification, NotificationJpaEntity entity) {
        if (notification.getUserId() != null) {
            UserJpaEntity userRef = entityManager.getReference(UserJpaEntity.class, notification.getUserId());
            entity.setUser(userRef);
        } else {
            entity.setUser(null);
        }

        if (notification.getFanpageId() != null) {
            FanpageJpaEntity fanpageRef = entityManager.getReference(FanpageJpaEntity.class, notification.getFanpageId());
            entity.setFanpage(fanpageRef);
        } else {
            entity.setFanpage(null);
        }
    }

    @Override
    public Notification save(Notification notification) {
        NotificationJpaEntity entity = notificationMapper.toJpaEntity(notification);
        if (notification.getId() != null) {
            entity.setId(notification.getId());
        }
        assignReferences(notification, entity);
        NotificationJpaEntity saved = notificationJpaRepository.save(entity);
        return notificationMapper.toDomainModel(saved);
    }

    @Override
    public List<Notification> saveAll(List<Notification> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        List<NotificationJpaEntity> jpaEntities = new ArrayList<>();
        for (Notification notification : entities) {
            NotificationJpaEntity entity = notificationMapper.toJpaEntity(notification);
            if (notification.getId() != null) {
                entity.setId(notification.getId());
            }
            assignReferences(notification, entity);
            jpaEntities.add(entity);
        }
        List<NotificationJpaEntity> savedEntities = notificationJpaRepository.saveAll(jpaEntities);
        return savedEntities.stream()
                .map(notificationMapper::toDomainModel)
                .toList();
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

    @Override
    public Page<Notification> findAllByFanpageId(Long fanpageId, Pageable pageable) {
        return notificationJpaRepository.findByFanpage_IdAndIsActiveTrue(fanpageId, pageable)
                .map(notificationMapper::toDomainModel);
    }

    @Override
    public Page<Notification> findAllByFanpageIdAndStatus(Long fanpageId, NotificationStatusEnum status, Pageable pageable) {
        return notificationJpaRepository.findByFanpage_IdAndStatusAndIsActiveTrue(fanpageId, status, pageable)
                .map(notificationMapper::toDomainModel);
    }

    @Override
    public long countUnreadByFanpageId(Long fanpageId) {
        return notificationJpaRepository.countByFanpage_IdAndStatusAndIsActiveTrue(fanpageId, NotificationStatusEnum.UNREAD);
    }

    @Override
    public void markAsReadByFanpage(Long id, Long fanpageId) {
        notificationJpaRepository.updateStatusByIdAndFanpageId(id, fanpageId, NotificationStatusEnum.READ);
    }

    @Override
    public void markAllAsReadByFanpage(Long fanpageId) {
        notificationJpaRepository.updateAllStatusByFanpageId(fanpageId, NotificationStatusEnum.UNREAD, NotificationStatusEnum.READ);
    }

    @Override
    public void deleteByIdAndFanpageId(Long id, Long fanpageId) {
        notificationJpaRepository.softDeleteByIdAndFanpageId(id, fanpageId);
    }


}
