package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentProfileJpaRepository extends JpaRepository<StudentProfileJpaEntity, Long> {
    boolean existsByStudentId(String studentId);

    @Query("SELECT s FROM StudentProfileJpaEntity s " +
            "JOIN FETCH s.user u JOIN FETCH s.major m JOIN FETCH s.association a JOIN FETCH m.school " +
            "WHERE u.email = :email")
    StudentProfileJpaEntity findByEmail(String email);
}
