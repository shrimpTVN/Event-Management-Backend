package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FanpageJpaRepository extends JpaRepository<FanpageJpaEntity, Long> {
    boolean existsByName(String name);
}
