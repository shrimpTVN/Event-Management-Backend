package com.ddd.application.dto.pointcategory;

import com.ddd.domain.model.PointCategory;

import java.util.List;
import java.util.Map;

public record CategoryGroup(
        Map<Long, List<PointCategory>> childrenByParent,
        List<PointCategory> roots
) {
}
