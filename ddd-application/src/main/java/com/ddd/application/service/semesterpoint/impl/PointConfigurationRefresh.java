package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.model.PointCategory;
import com.ddd.domain.model.StudentSemesterPointScope;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/** Captures immutable calculation inputs before an administrative mutation. */
@Component
@RequiredArgsConstructor
public class PointConfigurationRefresh {
    private final StudentSemesterPointRepository points;
    private final SemesterPointCategoryPolicy policy;
    private final StudentSemesterPointCommandService command;

    public record CategoryState(Long id, Long parentId, Integer maximum, boolean maxChild) {}

    public Map<Long,List<CategoryState>> capture() {
        Map<Long,List<CategoryState>> result=new HashMap<>();
        for (var scope : points.findAllScopes()) {
            result.computeIfAbsent(scope.semesterId(), id -> policy.effectiveCategories(id).stream()
                    .sorted(Comparator.comparing(PointCategory::getId))
                    .map(c -> new CategoryState(c.getId(),c.getParentCategoryId(),c.getMaximum(),c.isMaxChild())).toList());
        }
        return result;
    }

    public void refresh(Map<Long,List<CategoryState>> before) {
        var after=capture();
        var bySemester=points.findAllScopes().stream().collect(Collectors.groupingBy(StudentSemesterPointScope::semesterId));
        for (var entry : bySemester.entrySet()) {
            if (!Objects.equals(before.get(entry.getKey()),after.get(entry.getKey()))) {
                command.rebuildSemesterPoints(entry.getValue().stream().map(StudentSemesterPointScope::userId).distinct().sorted().toList(),entry.getKey());
            }
        }
    }
}
