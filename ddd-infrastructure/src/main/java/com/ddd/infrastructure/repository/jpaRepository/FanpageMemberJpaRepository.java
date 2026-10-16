package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FanpageMemberJpaRepository extends JpaRepository<FanpageMemberJpaEntity, FanpageMemberJpaEntityId> {

    boolean existsByUserIdAndRoleAndIsActive(Long userId, String role, Boolean isActive);

    boolean existsByFanpageIdAndUserIdAndRoleAndIsActive(Long fanpageId, Long userId, String name, boolean b);

    boolean existsByFanpageIdAndUserIdAndIsActive(Long fanpageId, Long userId, boolean isActive);

    @EntityGraph(attributePaths = {"user"})
    List<FanpageMemberJpaEntity> findAllByFanpageIdAndIsActive(Long fanpageId, boolean isActive);

}
