package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressCommandService;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentCriteriaProgressCommandServiceImpl implements StudentCriteriaProgressCommandService {
    private final CriteriaRepository criteriaRepository;
    private final StudentCriteriaProgressRepository progressRepository;


    @Override
    public void markCompleted(Long userId, Long criteriaId, Long academicYearId) {
        if (userId == null) {
            return;
        }
        bulkMarkCompleted(List.of(userId), criteriaId, academicYearId);
    }

    @Override
    public void bulkMarkCompleted(List<Long> userIds, Long criteriaId, Long academicYearId) {
        if (userIds == null || userIds.isEmpty() || criteriaId == null || academicYearId == null) {
            return;
        }
        List<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctUserIds.isEmpty()) {
            log.info("Received empty or null userIds list, skipping processing for criteria: {}, academic year: {}", criteriaId, academicYearId);
            return;
        }

        int requiredOccurrences = resolveRequiredOccurrences(criteriaId);
        Map<Long, StudentCriteriaProgress> existingByUserId = progressRepository
                .findByUserIdsAndCriteriaIdAndAcademicYearId(distinctUserIds, criteriaId, academicYearId)
                .stream()
                .collect(Collectors.toMap(
                        StudentCriteriaProgress::getUserId,
                        p -> p,
                        (existing, replacement) -> existing
                ));

        Instant now = Instant.now();
        List<StudentCriteriaProgress> leafProgressesToSave = new ArrayList<>(distinctUserIds.size());
        List<Long> newlyCompletedUserIds = new ArrayList<>();

        for (Long userId : distinctUserIds) {
            StudentCriteriaProgress progress = existingByUserId.getOrDefault(
                    userId,
                    new StudentCriteriaProgress(userId, criteriaId, academicYearId)
            );
            boolean wasCompleted = Boolean.TRUE.equals(progress.getIsCompleted());
            int currentOccurrences = progress.getCompletedOccurrences() != null ? progress.getCompletedOccurrences() : 0;
            int newOccurrences = currentOccurrences + 1;
            boolean isNowCompleted = newOccurrences >= requiredOccurrences;

            progress.setCompletedOccurrences(newOccurrences);
            progress.setIsCompleted(isNowCompleted);
            if (!wasCompleted && isNowCompleted) {
                progress.setCompletedAt(now);
                newlyCompletedUserIds.add(userId);
            }
            leafProgressesToSave.add(progress);
        }

        progressRepository.saveAll(leafProgressesToSave);
        log.info("Batch updated leaf criteria progress for {} users ({} newly completed), criteria: {}, academic year: {}",
                leafProgressesToSave.size(), newlyCompletedUserIds.size(), criteriaId, academicYearId);

        if (!newlyCompletedUserIds.isEmpty()) {
            List<StudentCriteriaProgress> updatedProgressList = calculateUpdateProgressList(
                    newlyCompletedUserIds, criteriaId, academicYearId, now);
            if (!updatedProgressList.isEmpty()) {
                progressRepository.saveAll(updatedProgressList);
                log.info("Batch updated {} parent criteria progress records for {} users",
                        updatedProgressList.size(), newlyCompletedUserIds.size());
            }
        }
    }

    private int resolveRequiredOccurrences(Long criteriaId) {
        return criteriaRepository.findById(criteriaId)
                .map(Criteria::getRequiredOccurrences)
                .filter(req -> req > 0)
                .orElse(1);
    }

    private List<StudentCriteriaProgress> calculateUpdateProgressList(List<Long> eligibleUserIds,
                                                                      Long criteriaId, Long academicYearId, Instant now) {

        List<Criteria> criteriaList = criteriaRepository.findAllByIsActive(true);
        Map<Long, Criteria> criteriaMap = criteriaList.stream()
                .collect(Collectors.toMap(Criteria::getId, c -> c));

        Criteria leafCriteria = criteriaMap.get(criteriaId);
        if (leafCriteria == null || leafCriteria.getParentId() == null) {
            log.warn("Invalid criteria or missing parent for criteria: {}", criteriaId);
            return Collections.emptyList();
        }

        List<StudentCriteriaProgress> updatedProgressList = new ArrayList<>();

        // Batch load existing progress for all eligible users in this academic year
        List<StudentCriteriaProgress> allExistingProgresses = progressRepository
                .findAllByUserIdsAndAcademicYearId(eligibleUserIds, academicYearId);

        Map<Long, Map<Long, StudentCriteriaProgress>> userProgressMap = allExistingProgresses.stream()
                .collect(Collectors.groupingBy(
                        StudentCriteriaProgress::getUserId,
                        Collectors.toMap(StudentCriteriaProgress::getCriteriaId,
                                p -> p, (existing, replacement) -> existing)
                ));

        // For each eligible user, traverse up the criteria tree and update parent progress
        for (Long userId : eligibleUserIds) {
            Map<Long, StudentCriteriaProgress> studentCriteriaMap = userProgressMap
                    .computeIfAbsent(userId, k -> new HashMap<>());

            Long currentParentId = leafCriteria.getParentId();
            while (currentParentId != null) {
                Criteria parentCriteria = criteriaMap.get(currentParentId);

                if (parentCriteria == null) {
                    //reach the root criteria
                    break;
                }

                StudentCriteriaProgress parentProgress = studentCriteriaMap
                        .computeIfAbsent(currentParentId, id -> new StudentCriteriaProgress(userId, id, academicYearId));

                parentProgress.setCompletedChildrenCount(parentProgress.getCompletedChildrenCount() + 1);

                int requiredChildren = parentCriteria.getRequiredChildren() != null
                        ? parentCriteria.getRequiredChildren()
                        : 1;

                if (!Boolean.TRUE.equals(parentProgress.getIsCompleted())
                        && parentProgress.getCompletedChildrenCount() >= requiredChildren) {
                    parentProgress.setIsCompleted(true);
                    parentProgress.setCompletedAt(now);
                    log.info("Parent criteria progress marked as completed for user: {}, criteria: {}, academic year: {}",
                            userId, currentParentId, academicYearId);
                    // Continue propagation to grandparent
                    currentParentId = parentCriteria.getParentId();
                } else {
                    if (parentProgress.getIsCompleted() == null) {
                        parentProgress.setIsCompleted(false);
                    }
                    // Threshold not reached or parent already completed, stop propagation
                    currentParentId = null;
                }

                updatedProgressList.add(parentProgress);
            }
        }
        return updatedProgressList;
    }
    @Override
    public void markUncompleted(Long userId, Long criteriaId, Long academicYearId) {
        var progressOpt = progressRepository
                .findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId);

        if (progressOpt.isEmpty()) {
            log.info("No progress found for user: {}, criteria: {}, academic year: {}, skipping uncomplete",
                    userId, criteriaId, academicYearId);
            return;
        }

        StudentCriteriaProgress leafProgress = progressOpt.get();
        boolean wasCompleted = Boolean.TRUE.equals(leafProgress.getIsCompleted());
        int currentOccurrences = leafProgress.getCompletedOccurrences() != null ? leafProgress.getCompletedOccurrences() : 0;

        if (!wasCompleted && currentOccurrences <= 0) {
            log.info("Progress for user: {}, criteria: {}, academic year: {} is already uncompleted, skipping", userId, criteriaId, academicYearId);
            return;
        }

        int newOccurrences = Math.max(0, currentOccurrences - 1);
        int requiredOccurrences = resolveRequiredOccurrences(criteriaId);
        boolean isNowCompleted = newOccurrences >= requiredOccurrences;

        if (wasCompleted && !isNowCompleted) {
            propagateUncompletedToParents(userId, criteriaId, academicYearId);
        }

        if (newOccurrences == 0) {
            progressRepository.delete(leafProgress);
        } else {
            leafProgress.setCompletedOccurrences(newOccurrences);
            leafProgress.setIsCompleted(isNowCompleted);
            if (!isNowCompleted) {
                leafProgress.setCompletedAt(null);
            }
            progressRepository.save(leafProgress);
        }
    }

    private void propagateUncompletedToParents(Long userId, Long criteriaId, Long academicYearId) {
        List<StudentCriteriaProgress> criteriaProgresses = progressRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId);
        Map<Long, StudentCriteriaProgress> progressMap = criteriaProgresses.stream()
                .collect(Collectors.toMap(StudentCriteriaProgress::getCriteriaId, p -> p));

        List<Long> criteriaIds = criteriaProgresses.stream()
                .map(StudentCriteriaProgress::getCriteriaId).distinct().toList();
        Map<Long, Criteria> criteriaMap = criteriaRepository.findAllById(criteriaIds).stream()
                .collect(Collectors.toMap(Criteria::getId, c -> c));

        List<StudentCriteriaProgress> updatedProgressList = new ArrayList<>();
        Criteria leafCriteria = criteriaMap.get(criteriaId);
        Long currentParentId = leafCriteria != null ? leafCriteria.getParentId() : null;

        while (currentParentId != null) {
            Criteria parentCriteria = criteriaMap.get(currentParentId);
            if (parentCriteria == null) {
                break;
            }

            StudentCriteriaProgress parentProgress = progressMap.get(currentParentId);
            if (parentProgress == null) {
                break;
            }

            int completedChildrenCount = Math.max(0, parentProgress.getCompletedChildrenCount() - 1);
            parentProgress.setCompletedChildrenCount(completedChildrenCount);

            int requiredChildren = parentCriteria.getRequiredChildren() != null
                    ? parentCriteria.getRequiredChildren()
                    : 1;

            if (parentProgress.getCompletedChildrenCount() < requiredChildren) {
                parentProgress.setIsCompleted(false);
                parentProgress.setCompletedAt(null);
                currentParentId = parentCriteria.getParentId();
            } else {
                currentParentId = null;
            }

            updatedProgressList.add(parentProgress);
        }

        if (!updatedProgressList.isEmpty()) {
            progressRepository.saveAll(updatedProgressList);
        }
    }


}
