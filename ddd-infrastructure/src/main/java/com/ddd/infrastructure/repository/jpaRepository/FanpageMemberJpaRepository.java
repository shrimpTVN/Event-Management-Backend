package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import com.ddd.infrastructure.repository.projection.FanpageMemberProjection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FanpageMemberJpaRepository extends JpaRepository<FanpageMemberJpaEntity, FanpageMemberJpaEntityId> {

    boolean existsByUserIdAndRoleAndIsActive(Long userId, String role, Boolean isActive);

    boolean existsByFanpageIdAndUserIdAndRoleAndIsActive(Long fanpageId, Long userId, String name, boolean b);

    @EntityGraph("user")
    List<FanpageMemberJpaEntity> findAllByFanpageIdAndIsActive(Long fanpageId, boolean b);

    @Query("""
        SELECT 
            u.id AS userId,
            u.email AS email,
            COALESCE(sp.firstName, fap.firstName) AS firstName,
            COALESCE(sp.lastName, fap.lastName) AS lastName,
            COALESCE(sp.avatarUrl, fap.avatarUrl) AS avatarUrl,
            fm.role AS role
        FROM FanpageMemberJpaEntity fm
        JOIN fm.user u
        LEFT JOIN StudentProfileJpaEntity sp ON sp.user = u
        LEFT JOIN FanpageAdminProfileJpaEntity fap ON fap.user = u
        WHERE fm.id.fanpageId = :fanpageId AND fm.isActive = true
    """)
    List<FanpageMemberProjection> findMembersWithProfileByFanpageId(@Param("fanpageId") Long fanpageId);
}
