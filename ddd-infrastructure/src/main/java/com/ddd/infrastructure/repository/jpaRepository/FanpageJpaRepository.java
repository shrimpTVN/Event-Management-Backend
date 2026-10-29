package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FanpageJpaRepository extends JpaRepository<FanpageJpaEntity, Long>, JpaSpecificationExecutor<FanpageJpaEntity> {
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Optional<FanpageJpaEntity> findByIdAndIsActive(Long id, boolean isActive);

    List<FanpageJpaEntity> findAllByIdInAndIsActive(List<Long> ids, boolean isActive);
}
