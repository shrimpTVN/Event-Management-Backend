package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.Semester;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
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
    private final SemesterRepository semesterRepository;

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

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + semesterId));
        LocalDate effectiveDate = semester.getEndDate() != null
                ? semester.getEndDate()
                : (semester.getStartDate() != null ? semester.getStartDate() : LocalDate.now());

        List<PointCategory> allCategories = pointCategoryRepository.findActiveEffectiveAsOf(effectiveDate);
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

                // 1. Update or create leaf StudentSemesterPoint (raw total retained for delta deductions)
                StudentSemesterPoint leafPoint = studentMap.computeIfAbsent(
                        leafCategoryId,
                        id -> new StudentSemesterPoint(userId, semesterId, id, 0)
                );
                updateLeafPoint(leafPoint, addedPoints, type, now);
                updatedForUser.put(leafCategoryId, leafPoint);

                // 2. Propagate effective (clamped) points to all ancestors upwards
                PointCategory leafCategory = categoryMap.get(leafCategoryId);
                propagateToAncestors(
                        userId,
                        semesterId,
                        leafCategory,
                        categoryMap,
                        childrenByParent,
                        studentMap,
                        updatedForUser,
                        now
                );
            }
            allPointsToSave.addAll(updatedForUser.values());
        }

        if (!allPointsToSave.isEmpty()) {
            studentSemesterPointRepository.saveAll(allPointsToSave);
            log.info("Persisted and propagated {} StudentSemesterPoint records across {} users for semester {}",
                    allPointsToSave.size(), userIds.size(), semesterId);
        }
    }

    private void updateLeafPoint(StudentSemesterPoint leafPoint,
                                 int addedPoints,
                                 PointTransactionTypeEnum type,
                                 Instant now) {
        if (type == PointTransactionTypeEnum.CHECKOUT_REWARD) {
            int currentSys = intValue(leafPoint.getSystemPointRecord());
            leafPoint.setSystemPointRecord(Math.max(0, currentSys + addedPoints));
        } else {
            int currentManual = intValue(leafPoint.getManualPointRecord());
            leafPoint.setManualPointRecord(Math.max(0, currentManual + addedPoints));
        }

        int sys = intValue(leafPoint.getSystemPointRecord());
        int manual = intValue(leafPoint.getManualPointRecord());
        leafPoint.setTotalPoint(sys + manual);
        leafPoint.setUpdatedAt(now);
    }

    private void propagateToAncestors(Long userId,
                                      Long semesterId,
                                      PointCategory leafCategory,
                                      Map<Long, PointCategory> categoryMap,
                                      Map<Long, List<PointCategory>> childrenByParent,
                                      Map<Long, StudentSemesterPoint> studentMap,
                                      Map<Long, StudentSemesterPoint> updatedForUser,
                                      Instant now) {
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
                applyMaxChildRollup(parentCategory, parentPoint, directChildren, studentMap);
            } else {
                applySumRollup(parentCategory, parentPoint, directChildren, studentMap);
            }

            parentPoint.setUpdatedAt(now);
            updatedForUser.put(currentParentId, parentPoint);
            currentParentId = parentCategory.getParentCategoryId();
        }
    }

    private void applyMaxChildRollup(PointCategory parentCategory,
                                     StudentSemesterPoint parentPoint,
                                     List<PointCategory> directChildren,
                                     Map<Long, StudentSemesterPoint> studentMap) {
        StudentSemesterPoint bestChildPoint = null;
        Long bestChildId = null;
        int bestEffectiveTotal = 0;

        for (PointCategory child : directChildren) {
            StudentSemesterPoint childPoint = studentMap.get(child.getId());
            if (childPoint == null) {
                continue;
            }
            int childEffectiveTotal = effectiveTotal(childPoint, child);
            if (isBetterMaxChildCandidate(childEffectiveTotal, childPoint, child.getId(),
                    bestEffectiveTotal, bestChildPoint, bestChildId)) {
                bestEffectiveTotal = childEffectiveTotal;
                bestChildPoint = childPoint;
                bestChildId = child.getId();
            }
        }

        if (bestChildPoint != null && bestEffectiveTotal > 0) {
            int cappedParentTotal = clampToMaximum(bestEffectiveTotal, parentCategory.getMaximum());
            parentPoint.setTotalPoint(cappedParentTotal);
            parentPoint.setSystemPointRecord(clampToMaximum(intValue(bestChildPoint.getSystemPointRecord()), cappedParentTotal));
            parentPoint.setManualPointRecord(clampToMaximum(intValue(bestChildPoint.getManualPointRecord()), cappedParentTotal));
        } else {
            parentPoint.setTotalPoint(0);
            parentPoint.setSystemPointRecord(0);
            parentPoint.setManualPointRecord(0);
        }
    }

    private boolean isBetterMaxChildCandidate(int candidateEffectiveTotal,
                                              StudentSemesterPoint candidatePoint,
                                              Long candidateId,
                                              int bestEffectiveTotal,
                                              StudentSemesterPoint bestPoint,
                                              Long bestId) {
        if (bestPoint == null) {
            return true;
        }
        if (candidateEffectiveTotal != bestEffectiveTotal) {
            return candidateEffectiveTotal > bestEffectiveTotal;
        }
        int candidateManual = intValue(candidatePoint.getManualPointRecord());
        int bestManual = intValue(bestPoint.getManualPointRecord());
        if (candidateManual != bestManual) {
            return candidateManual > bestManual;
        }
        if (candidateId == null) {
            return false;
        }
        return bestId == null || candidateId < bestId;
    }

    private void applySumRollup(PointCategory parentCategory,
                                StudentSemesterPoint parentPoint,
                                List<PointCategory> directChildren,
                                Map<Long, StudentSemesterPoint> studentMap) {
        int sumTotal = 0;
        int sumSys = 0;
        int sumManual = 0;
        for (PointCategory child : directChildren) {
            StudentSemesterPoint childPoint = studentMap.get(child.getId());
            if (childPoint != null) {
                sumTotal += effectiveTotal(childPoint, child);
                sumSys += clampToMaximum(intValue(childPoint.getSystemPointRecord()), child.getMaximum());
                sumManual += clampToMaximum(intValue(childPoint.getManualPointRecord()), child.getMaximum());
            }
        }

        Integer parentMax = parentCategory.getMaximum();
        parentPoint.setTotalPoint(clampToMaximum(sumTotal, parentMax));
        parentPoint.setSystemPointRecord(clampToMaximum(sumSys, parentMax));
        parentPoint.setManualPointRecord(clampToMaximum(sumManual, parentMax));
    }

    private int effectiveTotal(StudentSemesterPoint point, PointCategory category) {
        return clampToMaximum(intValue(point.getTotalPoint()), category != null ? category.getMaximum() : null);
    }

    private int clampToMaximum(int value, Integer maximum) {
        int nonNegative = Math.max(0, value);
        return maximum != null ? Math.min(maximum, nonNegative) : nonNegative;
    }

    private int intValue(Integer value) {
        return value != null ? value : 0;
    }
}
