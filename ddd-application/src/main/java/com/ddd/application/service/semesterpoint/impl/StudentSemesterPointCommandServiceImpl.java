package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentSemesterPointCommandServiceImpl implements StudentSemesterPointCommandService {
    private final StudentSemesterPointRepository studentSemesterPointRepository;
    private final PointCategoryRepository pointCategoryRepository;

    @Override
    public void recordPointAndPropagate(Long userId, Long semesterId, Long leafCategoryId, Integer points, PointTransactionTypeEnum type) {
        if (userId == null || semesterId == null || leafCategoryId == null || points == null || points == 0) {
            return;
        }
        bulkRecordPointsAndPropagate(List.of(userId), semesterId, Map.of(leafCategoryId, points), type);
    }

    @Override
    public void bulkRecordPointsAndPropagate(List<Long> userIds, Long semesterId,
                                             Map<Long, Integer> categoryPointsMap,
                                             PointTransactionTypeEnum type) {
        if (userIds == null || userIds.isEmpty() || semesterId == null || categoryPointsMap == null || categoryPointsMap.isEmpty()) {
            return;
        }

        List<PointCategory> allCategories = pointCategoryRepository.findAllActive();
        Map<Long, PointCategory> categoryMap = allCategories.stream()
                .collect(Collectors.toMap(PointCategory::getId, Function.identity()));

        Map<Long, List<PointCategory>> childrenByParent = allCategories.stream()
                .filter(c -> c.getParentCategoryId() != null && c.getParentCategoryId() != 0L)
                .collect(Collectors.groupingBy(PointCategory::getParentCategoryId));

        List<StudentSemesterPoint> existingPoints = studentSemesterPointRepository.findAllByUserIdsAndSemesterId(userIds, semesterId);
        Map<Long, Map<Long, StudentSemesterPoint>> userPointsMap = existingPoints.stream()
                .collect(Collectors.groupingBy(
                        StudentSemesterPoint::getUserId,
                        Collectors.toMap(StudentSemesterPoint::getPointCategoryId, Function.identity(), (a, b) -> a)
                ));

        Instant now = Instant.now();
        List<StudentSemesterPoint> allPointsToSave = new ArrayList<>();

        for (Long userId : userIds) {
            Map<Long, StudentSemesterPoint> studentMap = userPointsMap.computeIfAbsent(userId, k -> new HashMap<>());
            Map<Long, StudentSemesterPoint> updatedForUser = new LinkedHashMap<>();

            for (Map.Entry<Long, Integer> entry : categoryPointsMap.entrySet()) {
                Long leafCategoryId = entry.getKey();
                Integer addedPoints = entry.getValue();

                if (addedPoints == null || addedPoints == 0) {
                    continue;
                }

                // 1. Update or create leaf StudentSemesterPoint
                StudentSemesterPoint leafPoint = studentMap.computeIfAbsent(
                        leafCategoryId,
                        id -> new StudentSemesterPoint(userId, semesterId, id, 0)
                );

                if (type == PointTransactionTypeEnum.CHECKOUT_REWARD) {
                    int currentSys = leafPoint.getSystemPointRecord() != null ? leafPoint.getSystemPointRecord() : 0;
                    leafPoint.setSystemPointRecord(Math.max(0, currentSys + addedPoints));
                } else {
                    int currentManual = leafPoint.getManualPointRecord() != null ? leafPoint.getManualPointRecord() : 0;
                    leafPoint.setManualPointRecord(Math.max(0, currentManual + addedPoints));
                }

                int sys = leafPoint.getSystemPointRecord() != null ? leafPoint.getSystemPointRecord() : 0;
                int manual = leafPoint.getManualPointRecord() != null ? leafPoint.getManualPointRecord() : 0;
                leafPoint.setTotalPoint(sys + manual);
                leafPoint.setUpdatedAt(now);
                updatedForUser.put(leafCategoryId, leafPoint);

                // 2. Propagate to all ancestors upwards
                PointCategory leafCategory = categoryMap.get(leafCategoryId);
                Long currentParentId = leafCategory != null ? leafCategory.getParentCategoryId() : null;

                while (currentParentId != null && currentParentId != 0L) {
                    PointCategory parentCategory = categoryMap.get(currentParentId);
                    if (parentCategory == null) {
                        break;
                    }

                    List<PointCategory> directChildren = childrenByParent.getOrDefault(currentParentId, List.of());
                    StudentSemesterPoint parentPoint = studentMap.computeIfAbsent(
                            currentParentId,
                            id -> new StudentSemesterPoint(userId, semesterId, id, 0)
                    );

                    if (parentCategory.isMaxChild()) {
                        // Max rule: take the values of the direct child with highest totalPoint
                        StudentSemesterPoint bestChildPoint = null;
                        int maxTotal = 0;
                        for (PointCategory child : directChildren) {
                            StudentSemesterPoint childPoint = studentMap.get(child.getId());
                            if (childPoint != null) {
                                int childTotal = childPoint.getTotalPoint() != null ? childPoint.getTotalPoint() : 0;
                                if (bestChildPoint == null || childTotal > maxTotal) {
                                    maxTotal = childTotal;
                                    bestChildPoint = childPoint;
                                }
                            }
                        }

                        if (bestChildPoint != null) {
                            parentPoint.setTotalPoint(bestChildPoint.getTotalPoint() != null ? bestChildPoint.getTotalPoint() : 0);
                            parentPoint.setSystemPointRecord(bestChildPoint.getSystemPointRecord() != null ? bestChildPoint.getSystemPointRecord() : 0);
                            parentPoint.setManualPointRecord(bestChildPoint.getManualPointRecord() != null ? bestChildPoint.getManualPointRecord() : 0);
                        } else {
                            parentPoint.setTotalPoint(0);
                            parentPoint.setSystemPointRecord(0);
                            parentPoint.setManualPointRecord(0);
                        }
                    } else {
                        // Sum rule: sum values of all direct children
                        int sumTotal = 0;
                        int sumSys = 0;
                        int sumManual = 0;
                        for (PointCategory child : directChildren) {
                            StudentSemesterPoint childPoint = studentMap.get(child.getId());
                            if (childPoint != null) {
                                sumTotal += (childPoint.getTotalPoint() != null ? childPoint.getTotalPoint() : 0);
                                sumSys += (childPoint.getSystemPointRecord() != null ? childPoint.getSystemPointRecord() : 0);
                                sumManual += (childPoint.getManualPointRecord() != null ? childPoint.getManualPointRecord() : 0);
                            }
                        }

                        parentPoint.setTotalPoint(sumTotal);
                        parentPoint.setSystemPointRecord(sumSys);
                        parentPoint.setManualPointRecord(sumManual);
                    }

                    parentPoint.setUpdatedAt(now);
                    updatedForUser.put(currentParentId, parentPoint);
                    currentParentId = parentCategory.getParentCategoryId();
                }
            }
            allPointsToSave.addAll(updatedForUser.values());
        }

        if (!allPointsToSave.isEmpty()) {
            studentSemesterPointRepository.saveAll(allPointsToSave);
            log.info("Persisted and propagated {} StudentSemesterPoint records across {} users for semester {}",
                    allPointsToSave.size(), userIds.size(), semesterId);
        }
    }
}
