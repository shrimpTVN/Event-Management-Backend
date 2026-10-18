package com.ddd.infrastructure.repository;

import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.infrastructure.entity.StudentSemesterPointJpaEntity;
import com.ddd.infrastructure.mapper.StudentSemesterPointMapper;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentSemesterPointRepositoryImpl implements StudentSemesterPointRepository {
    private final StudentSemesterPointJpaRepository jpaRepository;
    private final StudentSemesterPointMapper mapper;
    private final EntityManager entityManager;

    @Override
    public StudentSemesterPoint findByUserIdAndSemesterIdAndPointCategoryId(Long userId,
                                                                            Long semesterId, Long pointCategoryId) {
        return mapper.toDomain(jpaRepository.findByUserIdAndSemesterIdAndPointCategoryId(userId,
                semesterId, pointCategoryId));
    }

    @Override
    public List<StudentSemesterPoint> findAllIn(List<Long> studentUserIds, Long semesterId, List<Long> pointCategoryIds) {
        if (studentUserIds == null || studentUserIds.isEmpty() || pointCategoryIds == null || pointCategoryIds.isEmpty()) {
            return List.of();
        }
        return mapper.toDomainList(jpaRepository.findAllIn(studentUserIds, semesterId, pointCategoryIds));
    }

    @Override
    public void saveAll(ArrayList<StudentSemesterPoint> studentSemesterPoints) {
        if (studentSemesterPoints == null || studentSemesterPoints.isEmpty()) {
            return;
        }

        List<StudentSemesterPointJpaEntity> entities = new ArrayList<>();
        for (StudentSemesterPoint studentSemesterPoint : studentSemesterPoints) {
            StudentSemesterPointJpaEntity entity = mapper.toJpaEntity(studentSemesterPoint);
            setAssociationReferences(studentSemesterPoint, entity);
            entities.add(entity);
        }

        jpaRepository.saveAll(entities);
    }

    @Override
    public void save(StudentSemesterPoint studentSemesterPoint) {
        StudentSemesterPointJpaEntity entity = mapper.toJpaEntity(studentSemesterPoint);
        setAssociationReferences(studentSemesterPoint, entity);
        jpaRepository.save(entity);
    }

    private void setAssociationReferences(StudentSemesterPoint studentSemesterPoint, StudentSemesterPointJpaEntity entity) {
        if (studentSemesterPoint.getUserId() != null) {
            entity.setUser(entityManager.getReference(com.ddd.infrastructure.entity.UserJpaEntity.class,
                    studentSemesterPoint.getUserId()));
        }
        if (studentSemesterPoint.getSemesterId() != null) {
            entity.setSemester(entityManager.getReference(com.ddd.infrastructure.entity.SemesterJpaEntity.class,
                    studentSemesterPoint.getSemesterId()));
        }
        if (studentSemesterPoint.getPointCategoryId() != null) {
            entity.setPointCategory(entityManager.getReference(com.ddd.infrastructure.entity.PointCategoryJpaEntity.class,
                    studentSemesterPoint.getPointCategoryId()));
        }
    }
}
