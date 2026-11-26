package com.ddd.application.service.criteriaprogress.impl;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
@RequiredArgsConstructor
public class CriteriaProgressLock {
    private final EntityManager entityManager;
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire(Long userId, Long academicYearId) {
        // Transaction-scoped advisory lock also protects absent progress rows.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))")
                .setParameter("key", "criteria-progress:" + userId + ":" + academicYearId).getSingleResult();
    }
}
