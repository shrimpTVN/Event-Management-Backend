package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressCommandService;
import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.repository.StudentCriteriaProgressRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentCriteriaProgressCommandServiceImpl implements StudentCriteriaProgressCommandService {
    private final CriteriaService criteriaService;
    private final StudentCriteriaProgressRepository progressRepository;
    private final StudentCriteriaTransactionJpaRepository transactions;
    private final CriteriaProgressLock progressLock;

    @Override
    public void markCompleted(Long userId, Long criteriaId, Long academicYearId) {
        if (userId != null) bulkMarkCompleted(List.of(userId), criteriaId, academicYearId);
    }
    @Override
    public void markUncompleted(Long userId, Long criteriaId, Long academicYearId) {
        markCompleted(userId, criteriaId, academicYearId);
    }
    @Override
    public void bulkMarkCompleted(List<Long> userIds, Long criteriaId, Long academicYearId) {
        if (userIds == null || academicYearId == null || criteriaId == null) return;
        for (Long userId : userIds.stream().filter(Objects::nonNull).distinct().sorted().toList()) {
            progressLock.acquire(userId, academicYearId);
            var tree = CriteriaProgressProjection.build(criteriaService.getCriteriaHierarchy(null),
                    transactions.findByUserIdAndAcademicYearId(userId, academicYearId));
            var existing = progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId).stream()
                    .collect(Collectors.toMap(StudentCriteriaProgress::getCriteriaId, Function.identity()));
            List<StudentCriteriaProgress> changed = new ArrayList<>();
            collect(tree, userId, academicYearId, existing, changed);
            progressRepository.saveAll(changed);
            // Categories removed from the effective tree must not retain stale completion.
            for (var stale : existing.values()) {
                stale.setIsCompleted(false);
                stale.setCompletedChildrenCount(0);
                stale.setCompletedOccurrences(0);
                stale.setCompletedAt(null);
                progressRepository.save(stale);
            }
        }
    }
    private void collect(List<CriteriaProgressItemDto> nodes, Long userId, Long yearId,
            Map<Long, StudentCriteriaProgress> existing, List<StudentCriteriaProgress> changed) {
        for (var node : nodes) {
            var point = existing.remove(node.id());
            if (point == null) point = new StudentCriteriaProgress(userId, node.id(), yearId);
            if (!node.isCompleted()) point.setCompletedAt(null);
            else if (!Boolean.TRUE.equals(point.getIsCompleted())) point.setCompletedAt(Instant.now());
            point.setIsCompleted(node.isCompleted());
            point.setCompletedOccurrences(node.completedOccurrences());
            point.setCompletedChildrenCount(node.completedChildrenCount());
            changed.add(point);
            collect(node.children(), userId, yearId, existing, changed);
        }
    }
}
