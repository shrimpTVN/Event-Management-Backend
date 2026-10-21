package com.ddd.application.service.criteria.impl;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.LeafProgressDetailDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.StudentCriteriaProgressQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.CriteriaJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.CriteriaJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaProgressJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * High-performance query service directly accessing JPA repositories for read-only evaluations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCriteriaProgressQueryServiceImpl implements StudentCriteriaProgressQueryService {

    private static final Comparator<CriteriaJpaEntity> CRITERIA_COMPARATOR =
            Comparator.comparing(CriteriaJpaEntity::getChildrenOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(CriteriaJpaEntity::getId, Comparator.nullsLast(Long::compareTo));

    private final CriteriaJpaRepository criteriaJpaRepository;
    private final StudentCriteriaProgressJpaRepository progressJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AcademicYearJpaRepository academicYearJpaRepository;

    @Override
    public StudentCriteriaProgressDto getProgress(String email, Long academicYearId) {
        UserJpaEntity user = findUser(email);
        AcademicYearJpaEntity academicYear = resolveAcademicYear(academicYearId);

        List<CriteriaJpaEntity> activeCriteria = getActiveCriteria();
        Map<Long, List<CriteriaJpaEntity>> childrenByParent = groupChildrenByParent(activeCriteria);

        List<StudentCriteriaProgressJpaEntity> progressList = loadProgressList(user.getId(), academicYear.getId());
        Set<Long> completedCriteriaIds = extractCompletedCriteriaIds(progressList);

        Map<Long, Boolean> completionCache = buildCompletionCache(activeCriteria, childrenByParent, completedCriteriaIds);

        List<CriteriaJpaEntity> mainCriteria = resolveMainCriteria(activeCriteria, childrenByParent);
        List<CriteriaProgressItemDto> criteriaTree = buildCriteriaTree(mainCriteria, childrenByParent, completionCache);
        List<LeafProgressDetailDto> progressDetails = buildProgressDetails(progressList);

        int completedMain = countCompletedCriteria(mainCriteria, completionCache);

        return new StudentCriteriaProgressDto(
                academicYear.getId(),
                academicYear.getCode(),
                completedMain,
                mainCriteria.size(),
                criteriaTree,
                progressDetails
        );
    }

    @Override
    public CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId) {
        StudentCriteriaProgressDto overallProgress = getProgress(email, academicYearId);
        return findCriteriaInTree(overallProgress.criterias(), criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + criteriaId));
    }

    private UserJpaEntity findUser(String email) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }

    private List<CriteriaJpaEntity> getActiveCriteria() {
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "level")
                .and(Sort.by(Sort.Direction.ASC, "childrenOrder"))
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return criteriaJpaRepository.findAllByIsActive(true, defaultSort);
    }

    private AcademicYearJpaEntity resolveAcademicYear(Long academicYearId) {
        // If academicYearId is null, find the current
        if (academicYearId == null) {
            LocalDate now = LocalDate.now();
            return academicYearJpaRepository
                    .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(now, now)
                    .orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));
        }
        return academicYearJpaRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private Map<Long, List<CriteriaJpaEntity>> groupChildrenByParent(List<CriteriaJpaEntity> activeCriteria) {
        return activeCriteria.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));
    }

    private List<StudentCriteriaProgressJpaEntity> loadProgressList(Long userId, Long academicYearId) {
        return progressJpaRepository.findAllByUserIdAndAcademicYearId(userId, academicYearId);
    }

    /**
     * Extracts the set of criteria IDs that have at least one progress record.
     * A progress record's existence means the leaf criteria is completed.
     */
    private Set<Long> extractCompletedCriteriaIds(List<StudentCriteriaProgressJpaEntity> progressList) {
        return progressList.stream()
                .map(p -> p.getCriteria().getId())
                .collect(Collectors.toSet());
    }


    /**
     * Builds a cache mapping each criteria ID to its completion status.
     * Leaf criteria: completed if it has any progress record.
     * Parent criteria: completed if enough children are completed (based on requiredChildren).
     */
    private Map<Long, Boolean> buildCompletionCache(
            List<CriteriaJpaEntity> activeCriteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Set<Long> completedCriteriaIds) {

        Map<Long, Boolean> completionCache = new HashMap<>();
        for (CriteriaJpaEntity criteria : activeCriteria) {
            evaluateCriteriaCompletion(criteria, childrenByParent, completedCriteriaIds, completionCache);
        }
        return completionCache;
    }

    private boolean evaluateCriteriaCompletion(
            CriteriaJpaEntity criteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Set<Long> completedCriteriaIds,
            Map<Long, Boolean> completionCache) {

        if (completionCache.containsKey(criteria.getId())) {
            return completionCache.get(criteria.getId());
        }

        List<CriteriaJpaEntity> children = childrenByParent.get(criteria.getId());
        boolean completed;

        if (children == null || children.isEmpty()) {
            // Leaf: completed if any progress record exists for this criteria
            completed = completedCriteriaIds.contains(criteria.getId());
        } else {
            // Parent: completed if enough children are completed
            int completedChildrenCount = 0;
            for (CriteriaJpaEntity child : children) {
                if (evaluateCriteriaCompletion(child, childrenByParent, completedCriteriaIds, completionCache)) {
                    completedChildrenCount++;
                }
            }
            int required = criteria.getRequiredChildren() == null
                    ? children.size()
                    : criteria.getRequiredChildren();
            completed = completedChildrenCount >= required;
        }

        completionCache.put(criteria.getId(), completed);
        return completed;
    }

    /**
     * Resolves the "main" criteria to show at the top level of the tree.
     * If there is a single root with children, its children become the main criteria.
     */
    private List<CriteriaJpaEntity> resolveMainCriteria(
            List<CriteriaJpaEntity> activeCriteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent) {

        List<CriteriaJpaEntity> roots = activeCriteria.stream()
                .filter(c -> c.getParent() == null)
                .sorted(CRITERIA_COMPARATOR)
                .toList();

        if (roots.size() == 1
                && childrenByParent.containsKey(roots.get(0).getId())
                && !childrenByParent.get(roots.get(0).getId()).isEmpty()) {
            return childrenByParent.get(roots.get(0).getId())
                    .stream()
                    .sorted(CRITERIA_COMPARATOR)
                    .toList();
        }
        return roots;
    }

    /**
     * Builds a nested tree of CriteriaProgressItemDto from the given top-level criteria.
     */
    private List<CriteriaProgressItemDto> buildCriteriaTree(
            List<CriteriaJpaEntity> mainCriteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Map<Long, Boolean> completionCache) {

        return mainCriteria.stream()
                .sorted(CRITERIA_COMPARATOR)
                .map(c -> buildCriteriaNode(c, childrenByParent, completionCache))
                .toList();
    }

    private CriteriaProgressItemDto buildCriteriaNode(
            CriteriaJpaEntity criteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Map<Long, Boolean> completionCache) {

        List<CriteriaJpaEntity> children = childrenByParent.getOrDefault(criteria.getId(), List.of());

        if (children.isEmpty()) {
            boolean completed = completionCache.getOrDefault(criteria.getId(), false);
            return new CriteriaProgressItemDto(
                    criteria.getId(),
                    criteria.getName(),
                    criteria.getNote(),
                    criteria.getScope(),
                    criteria.getLevel(),
                    criteria.getChildrenOrder(),
                    criteria.getRequiredChildren(),
                    0,
                    completed,
                    List.of()
            );
        }

        // Recursively build children nodes
        List<CriteriaProgressItemDto> childDtos = buildCriteriaTree(children, childrenByParent, completionCache);

        int completedChildrenCount = (int) childDtos.stream()
                .filter(CriteriaProgressItemDto::isCompleted)
                .count();

        boolean completed = completionCache.getOrDefault(criteria.getId(), false);

        return new CriteriaProgressItemDto(
                criteria.getId(),
                criteria.getName(),
                criteria.getNote(),
                criteria.getScope(),
                criteria.getLevel(),
                criteria.getChildrenOrder(),
                criteria.getRequiredChildren(),
                completedChildrenCount,
                completed,
                childDtos
        );
    }

    /**
     * Maps each progress record to a LeafProgressDetailDto.
     * Multiple records can exist per criteria (e.g. from different events).
     */
    private List<LeafProgressDetailDto> buildProgressDetails(List<StudentCriteriaProgressJpaEntity> progressList) {
        return progressList.stream()
                .map(p -> new LeafProgressDetailDto(
                        p.getCriteria().getId(),
                        p.getEvent() != null ? p.getEvent().getName() : null,
                        p.getNote(),
                        p.getCompletionSource()
                ))
                .toList();
    }

    private int countCompletedCriteria(List<CriteriaJpaEntity> criteria, Map<Long, Boolean> completionCache) {
        return (int) criteria.stream()
                .filter(c -> completionCache.getOrDefault(c.getId(), false))
                .count();
    }

    /**
     * Recursively searches the criteria tree for a node with the given ID.
     */
    private Optional<CriteriaProgressItemDto> findCriteriaInTree(List<CriteriaProgressItemDto> tree, Long criteriaId) {
        for (CriteriaProgressItemDto item : tree) {
            if (item.id().equals(criteriaId)) {
                return Optional.of(item);
            }
            Optional<CriteriaProgressItemDto> found = findCriteriaInTree(item.children(), criteriaId);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }
}
