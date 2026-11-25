package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointFlattenedItemDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import com.ddd.application.mapper.StudentSemesterPointDtoMapper;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.Semester;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentSemesterPointQueryServiceImpl implements StudentSemesterPointQueryService {

    private static final Comparator<PointCategory> POINT_CATEGORY_COMPARATOR = Comparator
            .comparing((PointCategory c) -> c.getLevel() != null ? c.getLevel() : 0)
            .thenComparing(c -> c.getChildrenOrder() != null ? c.getChildrenOrder() : 0)
            .thenComparing(PointCategory::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final StudentSemesterPointJpaRepository studentSemesterPointJpaRepository;
    private final StudentSemesterPointDtoMapper studentSemesterPointDtoMapper;
    private final UserJpaRepository userJpaRepository;
    private final StudentSemesterPointRepository studentSemesterPointRepository;
    private final PointCategoryRepository pointCategoryRepository;
    private final SemesterRepository semesterRepository;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<StudentSemesterPointInfoDto> getSemesterPointsForStudent(String email, Long semesterId) {
        UserJpaEntity user = userJpaRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        return studentSemesterPointJpaRepository.findByUserIdAndSemesterId(user.getId(), semesterId)
                .stream().map(studentSemesterPointDtoMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForCurrentStudent(Long semesterId) {
        Long userId = securityContextPort.getCurrentUserId();
        if (userId == null) {
            throw new AuthException("User is not authenticated");
        }
        return getSemesterPointTreeForStudent(userId, semesterId);
    }

    @Override
    public List<StudentSemesterPointGroupedTreeDto> getSemesterPointTreeForStudent(Long userId, Long semesterId) {
        if (userId == null || semesterId == null) {
            throw new IllegalArgumentException("User ID and Semester ID cannot be null");
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + semesterId));
        LocalDate effectiveDate = resolveEffectiveDate(semester);

        List<PointCategory> allCategories = pointCategoryRepository.findActiveEffectiveAsOf(effectiveDate);
        List<StudentSemesterPoint> studentPoints = studentSemesterPointRepository
                .findAllByUserIdAndSemesterId(userId, semesterId);

        Map<Long, StudentSemesterPoint> pointMap = studentPoints.stream()
                .filter(p -> p.getPointCategoryId() != null)
                .collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId, p -> p, (a, b) -> a));

        Map<Long, List<PointCategory>> childrenByParent = allCategories.stream()
                .filter(c -> c.getParentCategoryId() != null && c.getParentCategoryId() != 0L)
                .collect(Collectors.groupingBy(PointCategory::getParentCategoryId));

        List<PointCategory> roots = allCategories.stream()
                .filter(c -> c.getParentCategoryId() == null || c.getParentCategoryId() == 0L)
                .sorted(POINT_CATEGORY_COMPARATOR)
                .toList();

        return roots.stream().map(root -> {
            List<StudentSemesterPointFlattenedItemDto> flatItems = new ArrayList<>();
            flattenDescendantsDfs(root.getId(), childrenByParent, pointMap, flatItems);

            StudentSemesterPoint rootPoint = pointMap.get(root.getId());
            return new StudentSemesterPointGroupedTreeDto(
                    root.getId(),
                    root.getName(),
                    root.getNote(),
                    root.getMaximum(),
                    root.getDateApply(),
                    root.getLevel(),
                    root.getParentCategoryId(),
                    root.isActive(),
                    root.getChildrenOrder(),
                    root.isMaxChild(),
                    rootPoint != null ? rootPoint.getId() : null,
                    clampToMaximum(rootPoint != null ? rootPoint.getTotalPoint() : null, root.getMaximum()),
                    clampToMaximum(rootPoint != null ? rootPoint.getSystemPointRecord() : null, root.getMaximum()),
                    clampToMaximum(rootPoint != null ? rootPoint.getManualPointRecord() : null, root.getMaximum()),
                    rootPoint != null ? rootPoint.getFinalizedAt() : null,
                    flatItems
            );
        }).toList();
    }

    private void flattenDescendantsDfs(Long parentId,
                                       Map<Long, List<PointCategory>> childrenByParent,
                                       Map<Long, StudentSemesterPoint> pointMap,
                                       List<StudentSemesterPointFlattenedItemDto> resultList) {
        List<PointCategory> directChildren = childrenByParent.getOrDefault(parentId, List.of()).stream()
                .sorted(POINT_CATEGORY_COMPARATOR)
                .toList();

        directChildren.forEach(child -> {
            boolean isLeaf = !childrenByParent.containsKey(child.getId())
                    || childrenByParent.get(child.getId()).isEmpty();
            StudentSemesterPoint childPoint = pointMap.get(child.getId());

            resultList.add(new StudentSemesterPointFlattenedItemDto(
                    child.getId(),
                    child.getName(),
                    child.getNote(),
                    child.getMaximum(),
                    child.getDateApply(),
                    child.getLevel(),
                    child.getParentCategoryId(),
                    child.isActive(),
                    child.getChildrenOrder(),
                    isLeaf,
                    child.isMaxChild(),
                    childPoint != null ? childPoint.getId() : null,
                    clampToMaximum(childPoint != null ? childPoint.getTotalPoint() : null, child.getMaximum()),
                    clampToMaximum(childPoint != null ? childPoint.getSystemPointRecord() : null, child.getMaximum()),
                    clampToMaximum(childPoint != null ? childPoint.getManualPointRecord() : null, child.getMaximum()),
                    childPoint != null ? childPoint.getFinalizedAt() : null
            ));
            flattenDescendantsDfs(child.getId(), childrenByParent, pointMap, resultList);
        });
    }

    private LocalDate resolveEffectiveDate(Semester semester) {
        if (semester.getEndDate() != null) {
            return semester.getEndDate();
        }
        return semester.getStartDate() != null ? semester.getStartDate() : LocalDate.now();
    }

    private int clampToMaximum(Integer value, Integer maximum) {
        int nonNegative = value != null ? Math.max(0, value) : 0;
        return maximum != null ? Math.min(maximum, nonNegative) : nonNegative;
    }
}
