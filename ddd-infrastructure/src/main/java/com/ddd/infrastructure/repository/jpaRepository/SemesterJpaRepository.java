package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.SemesterJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemesterJpaRepository extends JpaRepository<SemesterJpaEntity, Long> {
    List<SemesterJpaEntity> findAllByIsActive(boolean isActive);
}
