package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface FanpageJpaRepository extends JpaRepository<FanpageJpaEntity, Long>, JpaSpecificationExecutor<FanpageJpaEntity> {
    boolean existsByName(String name);
}
