package com.ddd.infrastructure.repository;

import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.repository.PointTransactionRepository;
import com.ddd.infrastructure.entity.*;
import com.ddd.infrastructure.mapper.PointTransactionMapper;
import com.ddd.infrastructure.repository.jpaRepository.PointTransactionJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PointTransactionRepositoryImpl implements PointTransactionRepository {
    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final PointTransactionMapper pointTransactionMapper;
    private final EntityManager entityManager;
    @Override
    public void saveAll(List<PointTransaction> pointTransactions) {
        List<PointTransactionJpaEntity> pointTransactionJpaEntities = new ArrayList<>();

        for (PointTransaction pointTransaction : pointTransactions) {
            PointTransactionJpaEntity entity = pointTransactionMapper.toJpaEntity(pointTransaction);
            setAssociationReferences(pointTransaction, entity);
            pointTransactionJpaEntities.add(entity);
        }

        pointTransactionJpaRepository.saveAll(pointTransactionJpaEntities);
    }

    private void setAssociationReferences(PointTransaction pointTransaction, PointTransactionJpaEntity entity) {
        if (pointTransaction.getUserId() != null) {
            entity.setUser(entityManager.getReference(UserJpaEntity.class, pointTransaction.getUserId()));
        }
        if (pointTransaction.getPointCategoryId() != null) {
            entity.setPointCategory(entityManager.getReference(PointCategoryJpaEntity.class, pointTransaction.getPointCategoryId()));
        }
        if (pointTransaction.getSemesterId() != null) {
            entity.setSemester(entityManager.getReference(SemesterJpaEntity.class, pointTransaction.getSemesterId()));
        }
        if(pointTransaction.getEventId() != null) {
            entity.setEvent(entityManager.getReference(EventJpaEntity.class, pointTransaction.getEventId()));
        }
    }
}
