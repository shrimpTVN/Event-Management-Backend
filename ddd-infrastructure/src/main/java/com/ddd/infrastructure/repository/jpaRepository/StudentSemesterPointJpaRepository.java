package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.StudentSemesterPointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StudentSemesterPointJpaRepository extends JpaRepository<StudentSemesterPointJpaEntity, Long> {
    StudentSemesterPointJpaEntity findByUserIdAndSemesterIdAndPointCategoryId(Long userId, Long semesterId, Long pointCategoryId);


    @Query("""
            SELECT ssp FROM StudentSemesterPointJpaEntity ssp
            WHERE ssp.user.id IN :studentUserIds
            AND ssp.semester.id = :semesterId
            AND ssp.pointCategory.id IN :pointCategoryIds
            """)
    List<StudentSemesterPointJpaEntity> findAllIn(@Param("studentUserIds") List<Long> studentUserIds,
                                                  @Param("semesterId") Long semesterId,
                                                  @Param("pointCategoryIds") List<Long> pointCategoryIds);

    List<StudentSemesterPointJpaEntity> findByUserIdAndSemesterId(Long id, Long semesterId);
}
