package com.ddd.infrastructure.repository;

import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.repository.StudentCriteriaProgressRepository;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.StudentCriteriaProgressMapper;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaProgressJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class StudentCriteriaProgressRepositoryImpl implements StudentCriteriaProgressRepository {
    private final StudentCriteriaProgressJpaRepository jpaRepository;
    private final StudentCriteriaProgressMapper mapper;
    private final EntityManager entityManager;

    @Override
    public List<StudentCriteriaProgress> findByUserIdAndAcademicYearId(Long userId, Long academicYearId) {
        return mapper.toDomainList(jpaRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId));
    }

    @Override
    public Optional<StudentCriteriaProgress> findByUserIdAndCriteriaIdAndAcademicYearId(
            Long userId, Long criteriaId, Long academicYearId) {
        return jpaRepository.findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId)
                .map(mapper::toDomain);
    }

    @Override
    public List<StudentCriteriaProgress> findByUserIdsAndCriteriaIdAndAcademicYearId(
            Collection<Long> userIds, Long criteriaId, Long academicYearId) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mapper.toDomainList(jpaRepository.findAllByUserIdInAndCriteriaIdAndAcademicYearId(
                userIds, criteriaId, academicYearId));
    }

    @Override
    public StudentCriteriaProgress save(StudentCriteriaProgress progress) {
        StudentCriteriaProgressJpaEntity jpaEntity = mapper.toJpaEntity(progress);
        assignReferences(progress, jpaEntity);

        return mapper.toDomain(jpaRepository.save(jpaEntity));
    }

    @Override
    public List<StudentCriteriaProgress> saveAll(List<StudentCriteriaProgress> progresses) {
        if (progresses == null || progresses.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentCriteriaProgressJpaEntity> jpaEntities = new ArrayList<>();
        progresses.forEach(progress -> {
            StudentCriteriaProgressJpaEntity jpaEntity = mapper.toJpaEntity(progress);
            assignReferences(progress, jpaEntity);
            jpaEntities.add(jpaEntity);
        });

        return mapper.toDomainList(jpaRepository.saveAll(jpaEntities));
    }

    private void assignReferences(StudentCriteriaProgress progress, StudentCriteriaProgressJpaEntity entity) {
       if ( progress.getUserId() != null) {
            entity.setUser(entityManager.getReference(UserJpaEntity.class, progress.getUserId()));
        }
        if (progress.getCriteriaId() != null) {
            entity.setCriteria(entityManager.getReference(CriteriaJpaEntity.class, progress.getCriteriaId()));
        }
        if (progress.getAcademicYearId() != null) {
            entity.setAcademicYear(entityManager.getReference(AcademicYearJpaEntity.class, progress.getAcademicYearId()));
        }
    }
}
