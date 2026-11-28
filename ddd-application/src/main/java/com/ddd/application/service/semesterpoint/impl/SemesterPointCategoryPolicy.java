package com.ddd.application.service.semesterpoint.impl;

import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.repository.PointCategoryRepository;
import com.ddd.domain.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SemesterPointCategoryPolicy {
    private final SemesterRepository semesters;
    private final PointCategoryRepository categories;

    public List<PointCategory> effectiveCategories(Long semesterId) {
        var semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found: " + semesterId));
        LocalDate date = semester.getEndDate() != null ? semester.getEndDate()
                : semester.getStartDate() != null ? semester.getStartDate() : LocalDate.now();
        List<PointCategory> effective = categories.findActiveEffectiveAsOf(date);
        var byId = effective.stream().filter(PointCategory::isActive)
                .collect(Collectors.toMap(PointCategory::getId, c -> c));
        java.util.Set<Long> reachable = new java.util.HashSet<>();
        for (var category : byId.values()) {
            java.util.Set<Long> visited = new java.util.HashSet<>();
            PointCategory current = category;
            while (current != null) {
                if (!visited.add(current.getId())) throw new BusinessException("Cycle in effective point categories");
                Long parent = current.getParentCategoryId();
                if (parent == null || parent == 0L) { reachable.addAll(visited); break; }
                current = byId.get(parent);
            }
        }
        return effective.stream().filter(c -> reachable.contains(c.getId())).toList();
    }

    public void requireLeaves(Long semesterId, Collection<Long> categoryIds) {
        if (categoryIds.isEmpty()) return;
        List<PointCategory> effective = effectiveCategories(semesterId);
        Set<Long> activeIds = effective.stream().filter(PointCategory::isActive)
                .map(PointCategory::getId).collect(Collectors.toSet());
        Set<Long> parents = effective.stream().filter(PointCategory::isActive)
                .map(PointCategory::getParentCategoryId).collect(Collectors.toSet());
        for (Long id : categoryIds) {
            if (id == null || !activeIds.contains(id) || parents.contains(id)) {
                throw new BusinessException("Points require an effective leaf category for semester " + semesterId + ": " + id);
            }
        }
    }
}
