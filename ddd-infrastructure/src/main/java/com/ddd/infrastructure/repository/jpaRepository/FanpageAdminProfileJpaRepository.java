package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FanpageAdminProfileJpaRepository extends JpaRepository<FanpageAdminProfileJpaEntity, Long> {
    boolean existsByStaffId(String staffId);

    @EntityGraph(attributePaths = {"user"})
    Optional<FanpageAdminProfileJpaEntity> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<FanpageAdminProfileJpaEntity> findByUserEmail(String email);
}
