package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.MajorJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface MajorJpaRepository extends JpaRepository<MajorJpaEntity, Long> {
    List<MajorJpaEntity> findBySchoolIdAndIsActiveTrue(Long schoolId);
}