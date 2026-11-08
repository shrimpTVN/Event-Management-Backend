package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointFlattenedItemDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import com.ddd.application.mapper.StudentSemesterPointDtoMapper;
import com.ddd.application.service.semesterpoint.StudentSemesterPointQueryService;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentSemesterPointJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        List<PointCategory> allCategories = pointCategoryRepository.findAllActive();
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
                    rootPoint != null && rootPoint.getTotalPoint() != null ? rootPoint.getTotalPoint() : 0,
                    rootPoint != null && rootPoint.getSystemPointRecord() != null ? rootPoint.getSystemPointRecord() : 0,
                    rootPoint != null && rootPoint.getManualPointRecord() != null ? rootPoint.getManualPointRecord() : 0,
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
                    childPoint != null && childPoint.getTotalPoint() != null ? childPoint.getTotalPoint() : 0,
                    childPoint != null && childPoint.getSystemPointRecord() != null ? childPoint.getSystemPointRecord() : 0,
                    childPoint != null && childPoint.getManualPointRecord() != null ? childPoint.getManualPointRecord() : 0,
                    childPoint != null ? childPoint.getFinalizedAt() : null
            ));
            flattenDescendantsDfs(child.getId(), childrenByParent, pointMap, resultList);
        });
    }
}
