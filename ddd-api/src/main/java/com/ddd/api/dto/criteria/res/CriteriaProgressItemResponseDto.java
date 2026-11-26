package com.ddd.api.dto.criteria.res;

import java.util.List;

public record CriteriaProgressItemResponseDto(
        Long id,
        String name,
        String note,
        String scope,
        Integer level,
        Integer childrenOrder,
        Integer requiredChildren,
        int completedChildrenCount,
        Integer requiredOccurrences,
        int completedOccurrences,
        boolean isCompleted,
        int totalChildren,
        boolean isLeaf,
        boolean isSystemRecorded,
        boolean isSystemCompleted,
        boolean isSelfEvaluated,
        String selfEvaluationNote,
        Long manualTransactionId,
        String completionSource,
        String systemEventName,
        int completionPercentage,
        List<CriteriaProgressItemResponseDto> children
) {}
