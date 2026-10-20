package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FanpageMemberJpaRepository extends JpaRepository<FanpageMemberJpaEntity, FanpageMemberJpaEntityId> {

    boolean existsByUserIdAndRoleAndIsActive(Long userId, String role, Boolean isActive);

    boolean existsByFanpageIdAndUserIdAndRoleAndIsActive(Long fanpageId, Long userId, String name, boolean b);

    boolean existsByFanpageIdAndUserIdAndIsActive(Long fanpageId, Long userId, boolean isActive);

    @EntityGraph(attributePaths = {"user"})
    List<FanpageMemberJpaEntity> findAllByFanpageIdAndIsActive(Long fanpageId, boolean isActive);

    @Query("SELECT fm.id.fanpageId FROM FanpageMemberJpaEntity fm WHERE fm.id.userId = :userId AND fm.role = :role AND fm.isActive = true")
    List<Long> findFanpageIdsByUserIdAndRole(@Param("userId") Long userId, @Param("role") String role);

    @Modifying
    @Query("UPDATE FanpageMemberJpaEntity fm SET fm.isActive = false WHERE fm.id.fanpageId = :fanpageId")
    void deactivateAllMembersByFanpageId(@Param("fanpageId") Long fanpageId);
}
