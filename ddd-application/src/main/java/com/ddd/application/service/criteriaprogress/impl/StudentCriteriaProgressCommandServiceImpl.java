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

/**
 * Command service handling write mutations and business rules for student criteria progress.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentCriteriaProgressCommandServiceImpl implements StudentCriteriaProgressCommandService {

    private final UserRepository userRepository;
    private final CriteriaRepository criteriaRepository;
    private final StudentCriteriaProgressRepository progressRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EventRepository eventRepository;
    private final SemesterRepository semesterRepository;


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

        //Identify and skip students who already completed this criteria
        List<Long> eligibleUserIds = getEligibleUserIds(userIds, criteriaId, academicYearId);
        if (eligibleUserIds.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        List<StudentCriteriaProgress> newLeafProgresses = eligibleUserIds.stream()
                .map(userId -> {
                    StudentCriteriaProgress progress = new StudentCriteriaProgress(userId, criteriaId, academicYearId);
                    progress.setIsCompleted(true);
                    progress.setCompletedAt(now);
                    return progress;
                })
                .toList();

        progressRepository.saveAll(newLeafProgresses);
        log.info("Batch created leaf criteria progress for {} users, criteria: {}, academic year: {}",
                eligibleUserIds.size(), criteriaId, academicYearId);

        //Propagate completion up the criteria tree for each student
        List<StudentCriteriaProgress> updatedProgressList = calculateUpdateProgressList(eligibleUserIds, criteriaId, academicYearId, now);

        if (!updatedProgressList.isEmpty()) {
            progressRepository.saveAll(updatedProgressList);
            log.info("Batch updated {} parent criteria progress records for {} users",
                    updatedProgressList.size(), eligibleUserIds.size());
        }
    }

    private List<Long> getEligibleUserIds(List<Long> userIds, Long criteriaId, Long academicYearId) {
        List<Long> distinctUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (distinctUserIds.isEmpty()) {
            log.info("Received empty or null userIds list, skipping processing for criteria: {}, academic year: {}", criteriaId, academicYearId);
            return Collections.emptyList();
        }

        Set<Long> alreadyCompletedUserIds = progressRepository
                .findByUserIdsAndCriteriaIdAndAcademicYearId(distinctUserIds, criteriaId, academicYearId)
                .stream()
                .map(StudentCriteriaProgress::getUserId)
                .collect(Collectors.toSet());

        List<Long> eligibleUserIds = distinctUserIds.stream()
                .filter(id -> !alreadyCompletedUserIds.contains(id))
                .toList();

        if (eligibleUserIds.isEmpty()) {
            log.info("All {} users already marked completed for criteria: {}, academic year: {}",
                    distinctUserIds.size(), criteriaId, academicYearId);
            return Collections.emptyList();
        }
        return eligibleUserIds;
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
                    // Threshold not reached or parent already completed, stop propagation
                    currentParentId = null;
                }

                updatedProgressList.add(parentProgress);
            }
        }
        return updatedProgressList;
    }
}
