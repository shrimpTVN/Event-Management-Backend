package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileJpaEntity,Long> {
    boolean existsByStudentId(String studentId);
}
