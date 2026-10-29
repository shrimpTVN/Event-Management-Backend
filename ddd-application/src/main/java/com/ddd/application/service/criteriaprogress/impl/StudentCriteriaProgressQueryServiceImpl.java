package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.StudentCriteriaProgressJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaProgressJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Query service for evaluating student criteria progress against the criteria hierarchy.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentCriteriaProgressQueryServiceImpl implements StudentCriteriaProgressQueryService {

    private final CriteriaService criteriaService;
    private final StudentCriteriaProgressJpaRepository progressJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AcademicYearJpaRepository academicYearJpaRepository;

    @Override
    public StudentCriteriaProgressDto getProgress(String email, Long academicYearId) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        AcademicYearJpaEntity academicYear = resolveAcademicYear(academicYearId);

        List<CriteriaTreeNodeDto> criteriaTree = criteriaService.getCriteriaTree();

        List<StudentCriteriaProgressJpaEntity> progressList = progressJpaRepository
                .findAllByUserIdAndAcademicYearId(user.getId(), academicYearId);
        Map<Long, StudentCriteriaProgressJpaEntity> progressMap = progressList.stream()
                .collect(Collectors.toMap(p -> p.getCriteria().getId(), p -> p, (existing, replacement) -> existing));


        List<CriteriaProgressItemDto> progressTree = criteriaTree.stream()
                .map(node -> attachProgress(node, progressMap)).toList();

        int completedMain = (int) progressTree.stream().filter(CriteriaProgressItemDto::isCompleted).count();

        return new StudentCriteriaProgressDto(academicYear.getId(), academicYear.getCode(), completedMain, progressTree.size(), progressTree);
    }

    @Override
    public CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId) {
        StudentCriteriaProgressDto overallProgress = getProgress(email, academicYearId);
        return findCriteriaInTree(overallProgress.criterias(), criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + criteriaId));
    }

    /**
     * Recursively traverses the tree top-down and computes node completion status.
     */
    private CriteriaProgressItemDto attachProgress(CriteriaTreeNodeDto node,
                                                   Map<Long, StudentCriteriaProgressJpaEntity> progressMap) {

        // Recursively build children first so their completion statuses are known
        List<CriteriaProgressItemDto> childDtos = node.children()
                .stream().map(child -> attachProgress(child, progressMap)).toList();

        int completedChildrenCount = progressMap.get(node.id()) != null ? progressMap.get(node.id()).getCompletedChildrenCount() : 0;
        boolean isCompleted = progressMap.get(node.id()) != null ? progressMap.get(node.id()).getIsCompleted() : false;

        return new CriteriaProgressItemDto(
                node.id(),
                node.name(),
                node.note(),
                node.scope(),
                node.level(),
                node.childrenOrder(),
                node.requiredChildren(),
                completedChildrenCount,
                isCompleted,
                childDtos);
    }

    private AcademicYearJpaEntity resolveAcademicYear(Long academicYearId) {
        if (academicYearId == null) {
            LocalDate now = LocalDate.now();
            return academicYearJpaRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(now, now).orElseThrow(() -> new ResourceNotFoundException("Current academic year not found"));
        }
        return academicYearJpaRepository.findById(academicYearId).orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

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
