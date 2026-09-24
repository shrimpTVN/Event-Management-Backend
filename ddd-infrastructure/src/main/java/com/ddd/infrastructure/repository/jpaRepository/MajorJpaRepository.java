package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.MajorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MajorJpaRepository extends JpaRepository<MajorJpaEntity, Long> {
}
