package com.ddd.infrastructure.repository;

import com.ddd.domain.model.StudentCriteriaTransaction;
import com.ddd.domain.repository.StudentCriteriaTransactionRepository;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.StudentCriteriaTransactionMapper;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StudentCriteriaTransactionRepositoryImpl implements StudentCriteriaTransactionRepository {
    private final StudentCriteriaTransactionJpaRepository jpaRepository;
    private final StudentCriteriaTransactionMapper mapper;
    private final EntityManager em;
    @Override
    public void save(StudentCriteriaTransaction transaction) {
        var transactionEntity = mapper.toEntity(transaction);
        assignReferences(transaction, transactionEntity);
        jpaRepository.save(transactionEntity);
    }

    @Override
    public void saveAll(List<StudentCriteriaTransaction> toSave) {
        List<StudentCriteriaTransactionJpaEntity> entities = new ArrayList<>();
        toSave.forEach(transaction -> {
            var transactionEntity = mapper.toEntity(transaction);
            assignReferences(transaction, transactionEntity);
            entities.add(transactionEntity);
        });
        jpaRepository.saveAll(entities);
    }

    private void assignReferences(StudentCriteriaTransaction transaction,
                                  StudentCriteriaTransactionJpaEntity transactionEntity) {
        if (transaction.getUserId() != null) {
            var userRef = em.getReference(UserJpaEntity.class, transaction.getUserId());
            transactionEntity.setUser(userRef);
        }
        if (transaction.getCriteriaId() != null) {
            var criteriaRef = em.getReference(CriteriaJpaEntity.class, transaction.getCriteriaId());
            transactionEntity.setCriteria(criteriaRef);
        }
        if (transaction.getAcademicYearId() != null) {
            var academicYearRef = em.getReference(AcademicYearJpaEntity.class, transaction.getAcademicYearId());
            transactionEntity.setAcademicYear(academicYearRef);
        }
    }

}
