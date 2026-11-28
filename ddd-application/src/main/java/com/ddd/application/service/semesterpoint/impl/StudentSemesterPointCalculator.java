package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.StudentSemesterPointCalculation;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointCalculation.NodeMetadata;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.model.StudentSemesterPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

/** Calculates snapshots from the ledger. Parent totals retain their value before their own cap. */
@Component
@Slf4j
public class StudentSemesterPointCalculator {
    public StudentSemesterPointCalculation calculate(Long userId, Long semesterId,
            List<PointCategory> categories, List<PointTransaction> transactions) {
        try {
            return new Evaluation(userId, semesterId, categories, transactions).calculate();
        } catch (ArithmeticException exception) {
            throw new BusinessException("Semester points exceed the supported integer range");
        }
    }

    private record SourceTotals(long system, long manual) {}

    private static final class Evaluation {
        private final Long userId;
        private final Long semesterId;
        private final Map<Long, PointCategory> categories = new TreeMap<>();
        private final Map<Long, List<Long>> children = new HashMap<>();
        private final Map<Long, SourceTotals> sources = new HashMap<>();
        private final Map<Long, StudentSemesterPoint> points = new LinkedHashMap<>();
        private final Map<Long, Long> winners = new HashMap<>();
        private final Set<Long> visiting = new HashSet<>();

        Evaluation(Long userId, Long semesterId, List<PointCategory> categoryList,
                   List<PointTransaction> transactions) {
            this.userId = Objects.requireNonNull(userId);
            this.semesterId = Objects.requireNonNull(semesterId);
            for (PointCategory category : categoryList) {
                if (!category.isActive()) continue;
                if (category.getId() == null || category.getMaximum() == null || category.getMaximum() < 0) {
                    throw new BusinessException("Point categories must have an ID and a non-negative maximum");
                }
                if (categories.put(category.getId(), category) != null) {
                    throw new BusinessException("Duplicate point category: " + category.getId());
                }
            }
            for (PointCategory category : categories.values()) {
                Long parent = category.getParentCategoryId();
                if (parent == null || parent == 0L) continue;
                if (!categories.containsKey(parent)) {
                    throw new BusinessException("Point category has no effective parent: " + category.getId());
                }
                children.computeIfAbsent(parent, key -> new ArrayList<>()).add(category.getId());
            }
            for (PointTransaction transaction : transactions) {
                if (!userId.equals(transaction.getUserId()) || !semesterId.equals(transaction.getSemesterId())) continue;
                Long categoryId = transaction.getPointCategoryId();
                if (categoryId == null || !categories.containsKey(categoryId) || children.containsKey(categoryId)) {
                    log.warn("Excluding point transaction {} for non-effective or non-leaf category {}", transaction.getId(), categoryId);
                    continue;
                }
                if (transaction.getPoints() == null) {
                    throw new BusinessException("Point transaction has no points: " + transaction.getId());
                }
                SourceTotals totals = sources.getOrDefault(categoryId, new SourceTotals(0, 0));
                if ("CHECKOUT_REWARD".equals(transaction.getType())) {
                    sources.put(categoryId, new SourceTotals(
                            Math.addExact(totals.system(), transaction.getPoints()), totals.manual()));
                } else {
                    sources.put(categoryId, new SourceTotals(totals.system(),
                            Math.addExact(totals.manual(), transaction.getPoints())));
                }
            }
        }

        StudentSemesterPointCalculation calculate() {
            for (Long id : categories.keySet()) evaluate(id);
            Map<Long, NodeMetadata> metadata = new LinkedHashMap<>();
            for (PointCategory category : categories.values()) {
                Long id = category.getId();
                int effective = effective(id);
                int maximum = category.getMaximum();
                Long parent = category.getParentCategoryId();
                boolean contributes = !winners.containsKey(parent) || id.equals(winners.get(parent));
                metadata.put(id, new NodeMetadata(maximum > 0 && effective >= maximum,
                        maximum == 0 ? 0 : (int) ((long) effective * 100 / maximum), winners.get(id), contributes));
            }
            return new StudentSemesterPointCalculation(List.copyOf(points.values()), Map.copyOf(metadata));
        }

        private StudentSemesterPoint evaluate(Long id) {
            if (points.containsKey(id)) return points.get(id);
            if (!visiting.add(id)) throw new BusinessException("Cycle in point category hierarchy at " + id);
            PointCategory category = categories.get(id);
            List<Long> childIds = children.getOrDefault(id, List.of());
            long system = 0;
            long manual = 0;
            long total = 0;
            if (childIds.isEmpty()) {
                SourceTotals values = sources.getOrDefault(id, new SourceTotals(0, 0));
                system = Math.max(0, values.system());
                manual = Math.max(0, values.manual());
                total = Math.addExact(system, manual);
            } else if (category.isMaxChild()) {
                StudentSemesterPoint best = null;
                for (Long childId : childIds) {
                    StudentSemesterPoint child = evaluate(childId);
                    if (best == null || better(child, best)) best = child;
                }
                winners.put(id, best.getPointCategoryId());
                system = best.getSystemPointRecord();
                manual = best.getManualPointRecord();
                total = effective(best.getPointCategoryId());
            } else {
                for (Long childId : childIds) {
                    StudentSemesterPoint child = evaluate(childId);
                    system = Math.addExact(system, child.getSystemPointRecord());
                    manual = Math.addExact(manual, child.getManualPointRecord());
                    total = Math.addExact(total, effective(childId));
                }
            }
            var point = new StudentSemesterPoint(userId, semesterId, id, Math.toIntExact(total));
            point.setSystemPointRecord(Math.toIntExact(system));
            point.setManualPointRecord(Math.toIntExact(manual));
            points.put(id, point);
            visiting.remove(id);
            return point;
        }

        private boolean better(StudentSemesterPoint candidate, StudentSemesterPoint best) {
            int comparison = Integer.compare(effective(candidate.getPointCategoryId()), effective(best.getPointCategoryId()));
            if (comparison != 0) return comparison > 0;
            comparison = Integer.compare(candidate.getManualPointRecord(), best.getManualPointRecord());
            if (comparison != 0) return comparison > 0;
            return candidate.getPointCategoryId() < best.getPointCategoryId();
        }

        private int effective(Long id) {
            return Math.min(categories.get(id).getMaximum(), points.get(id).getTotalPoint());
        }
    }
}
