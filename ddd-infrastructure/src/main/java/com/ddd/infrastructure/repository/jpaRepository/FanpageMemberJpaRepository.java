package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntityId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FanpageMemberJpaRepository extends JpaRepository<FanpageMemberJpaEntity, FanpageMemberJpaEntityId> {

    boolean existsByUserIdAndRoleAndIsActive(Long userId, String role, Boolean isActive);

    boolean existsByFanpageIdAndUserIdAndRoleAndIsActive(Long fanpageId, Long userId, String name, boolean b);

    @EntityGraph("user")
    List<FanpageMemberJpaEntity> findAllByFanpageIdAndIsActive(Long fanpageId, boolean b);
}
