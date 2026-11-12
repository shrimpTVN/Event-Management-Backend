package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.infrastructure.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, Long> {

    Page<NotificationJpaEntity> findByUser_IdAndIsActiveTrue(Long userId, Pageable pageable);

    Page<NotificationJpaEntity> findByUser_IdAndStatusAndIsActiveTrue(Long userId, NotificationStatusEnum status, Pageable pageable);

    long countByUser_IdAndStatusAndIsActiveTrue(Long userId, NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.status = :status WHERE n.id = :id AND n.user.id = :userId AND n.isActive = true")
    int updateStatusByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("status") NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.status = :status " +
            "WHERE n.user.id = :userId AND n.status = :targetStatus AND n.isActive = true")
    int updateAllStatusByUserId(@Param("userId") Long userId, @Param("targetStatus") NotificationStatusEnum targetStatus,
                                @Param("status") NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isActive = false WHERE n.id = :id AND n.user.id = :userId")
    int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Page<NotificationJpaEntity> findByFanpage_IdAndIsActiveTrue(Long fanpageId, Pageable pageable);

    Page<NotificationJpaEntity> findByFanpage_IdAndStatusAndIsActiveTrue(Long fanpageId, NotificationStatusEnum status, Pageable pageable);

    long countByFanpage_IdAndStatusAndIsActiveTrue(Long fanpageId, NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.status = :status WHERE n.id = :id AND n.fanpage.id = :fanpageId AND n.isActive = true")
    int updateStatusByIdAndFanpageId(@Param("id") Long id, @Param("fanpageId") Long fanpageId, @Param("status") NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.status = :status " +
            "WHERE n.fanpage.id = :fanpageId AND n.status = :targetStatus AND n.isActive = true")
    int updateAllStatusByFanpageId(@Param("fanpageId") Long fanpageId, @Param("targetStatus") NotificationStatusEnum targetStatus,
                                  @Param("status") NotificationStatusEnum status);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isActive = false WHERE n.id = :id AND n.fanpage.id = :fanpageId")
    int softDeleteByIdAndFanpageId(@Param("id") Long id, @Param("fanpageId") Long fanpageId);
}
