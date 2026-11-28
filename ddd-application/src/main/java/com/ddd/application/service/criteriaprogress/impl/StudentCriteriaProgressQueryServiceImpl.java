package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.CriteriaProgressItemDto;
import com.ddd.application.dto.criteria.CriteriaTreeNodeDto;
import com.ddd.application.dto.criteria.StudentCriteriaProgressDto;
import com.ddd.application.service.criteria.CriteriaService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressQueryService;
import com.ddd.domain.enums.CriteriaScopeEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.infrastructure.entity.AcademicYearJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Query service for evaluating student criteria progress against the criteria hierarchy.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
public class StudentCriteriaProgressQueryServiceImpl implements StudentCriteriaProgressQueryService {

    private final CriteriaService criteriaService;
    private final com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository transactions;
    private final UserJpaRepository userJpaRepository;
    private final AcademicYearJpaRepository academicYearJpaRepository;

    @Override
    public StudentCriteriaProgressDto getProgress(String email, Long academicYearId) {
        return getProgress(email, academicYearId, null);
    }

    @Override
    public StudentCriteriaProgressDto getProgress(String email, Long academicYearId, CriteriaScopeEnum scope) {
        var full = loadProgress(email, academicYearId, scope);
        var visible = full.criterias().stream().flatMap(node ->
                Integer.valueOf(0).equals(node.level()) && !node.children().isEmpty()
                        ? node.children().stream() : java.util.stream.Stream.of(node)).toList();
        int completed = (int) visible.stream().filter(CriteriaProgressItemDto::isCompleted).count();
        int total = visible.size();
        return new StudentCriteriaProgressDto(full.academicYearId(), full.academicYear(), completed, total,
                visible, full.scope(), total == 0 ? 0 : completed * 100 / total, total > 0 && completed == total);
    }

    private StudentCriteriaProgressDto loadProgress(String email, Long academicYearId, CriteriaScopeEnum scope) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
        AcademicYearJpaEntity academicYear = resolveAcademicYear(academicYearId);

        List<CriteriaTreeNodeDto> criteriaTree = criteriaService.getCriteriaHierarchy(scope);

        var records = transactions.findByUserIdAndAcademicYearId(user.getId(), academicYear.getId());
        var progressTree = CriteriaProgressProjection.build(criteriaTree, records);
        int completed = (int) progressTree.stream().filter(CriteriaProgressItemDto::isCompleted).count();
        int total = progressTree.size();
        return new StudentCriteriaProgressDto(academicYear.getId(), academicYear.getCode(), completed, total,
                progressTree, scope == null ? null : scope.name(), total == 0 ? 0 : completed * 100 / total,
                total > 0 && completed == total);
    }

    @Override
    public CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId) {
        return getCriteriaProgress(email, criteriaId, academicYearId, null);
    }

    @Override
    public CriteriaProgressItemDto getCriteriaProgress(String email, Long criteriaId, Long academicYearId, CriteriaScopeEnum scope) {
        StudentCriteriaProgressDto overallProgress = loadProgress(email, academicYearId, scope);
        return findCriteriaInTree(overallProgress.criterias(), criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found with id: " + criteriaId));
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
