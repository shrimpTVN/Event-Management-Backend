package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FollowingJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowingJpaRepository extends JpaRepository<FollowingJpaEntity, Long> {

    Optional<FollowingJpaEntity> findByFanpage_IdAndUser_Id(Long fanpageId, Long userId);

    long countByFanpage_IdAndFolStatusAndIsActive(Long fanpageId, FollowingStatusEnum folStatus, Boolean isActive);

    boolean existsByFanpage_IdAndUser_IdAndFolStatusAndIsActive(Long fanpageId, Long userId, FollowingStatusEnum folStatus, Boolean isActive);

    @Query(
            value = "SELECT f.fanpage FROM FollowingJpaEntity f " +
                    "WHERE f.user.id = :userId " +
                    "AND f.folStatus = :folStatus " +
                    "AND f.isActive = true " +
                    "AND f.fanpage.isActive = true " +
                    "AND f.fanpage.status = 'ACTIVE'",
            countQuery = "SELECT COUNT(f) FROM FollowingJpaEntity f " +
                    "WHERE f.user.id = :userId " +
                    "AND f.folStatus = :folStatus " +
                    "AND f.isActive = true " +
                    "AND f.fanpage.isActive = true " +
                    "AND f.fanpage.status = 'ACTIVE'"
    )
    Page<FanpageJpaEntity> findFollowingFanpagesByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("folStatus") FollowingStatusEnum folStatus,
            Pageable pageable
    );
}
