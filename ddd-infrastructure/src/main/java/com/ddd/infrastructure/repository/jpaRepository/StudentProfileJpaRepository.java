package com.ddd.infrastructure.repository.jpaRepository;

import aj.org.objectweb.asm.commons.Remapper;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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


    @EntityGraph(attributePaths = {"user", "major", "association", "major.school"})
    @Query("SELECT s FROM StudentProfileJpaEntity s WHERE s.user.isActive = :isActive")
    Page<StudentProfileJpaEntity> findAllByIsActive(boolean isActive, Pageable pageable);

}
