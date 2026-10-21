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
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        AcademicYearJpaEntity academicYear = resolveAcademicYear(academicYearId);

        List<CriteriaJpaEntity> activeCriteria = getActiveCriteria();
        Map<Long, List<CriteriaJpaEntity>> childrenByParent = groupChildrenByParent(activeCriteria);

        List<StudentCriteriaProgressJpaEntity> progressList = loadProgressList(user.getId(), academicYear.getId());
        Set<Long> completedCriteriaIds = extractCompletedCriteriaIds(progressList);

        List<CriteriaJpaEntity> mainCriteria = resolveMainCriteria(activeCriteria, childrenByParent);
        List<CriteriaProgressItemDto> criteriaTree = buildCriteriaTree(mainCriteria, childrenByParent, completedCriteriaIds);
        List<LeafProgressDetailDto> progressDetails = buildProgressDetails(progressList);

        int completedMain = (int) criteriaTree.stream()
                .filter(CriteriaProgressItemDto::isCompleted)
                .count();

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
     * Builds a nested tree of CriteriaProgressItemDto from the given top-level criteria in a single bottom-up pass.
     */
    private List<CriteriaProgressItemDto> buildCriteriaTree(
            List<CriteriaJpaEntity> mainCriteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Set<Long> completedCriteriaIds) {

        return mainCriteria.stream()
                .sorted(CRITERIA_COMPARATOR)
                .map(c -> buildCriteriaNode(c, childrenByParent, completedCriteriaIds))
                .toList();
    }

    private CriteriaProgressItemDto buildCriteriaNode(
            CriteriaJpaEntity criteria,
            Map<Long, List<CriteriaJpaEntity>> childrenByParent,
            Set<Long> completedCriteriaIds) {

        List<CriteriaJpaEntity> children = childrenByParent.getOrDefault(criteria.getId(), List.of());

        // Base case: Leaf criteria (evaluated directly against student progress records)
        if (children.isEmpty()) {
            boolean completed = completedCriteriaIds.contains(criteria.getId());
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

        // Recursive step: Parent criteria (evaluated bottom-up from children completion)
        List<CriteriaProgressItemDto> childDtos = buildCriteriaTree(children, childrenByParent, completedCriteriaIds);

        int completedChildrenCount = (int) childDtos.stream()
                .filter(CriteriaProgressItemDto::isCompleted)
                .count();

        int required = criteria.getRequiredChildren() == null
                ? childDtos.size()
                : criteria.getRequiredChildren();
        boolean completed = completedChildrenCount >= required;

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
