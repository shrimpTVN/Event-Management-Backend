package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentSemesterPointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSemesterJpaRepository extends JpaRepository<StudentSemesterPointJpaEntity, Long> {
}
