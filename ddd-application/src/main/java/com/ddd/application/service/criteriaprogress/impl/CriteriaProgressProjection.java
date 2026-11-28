package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.*;
import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import java.util.*;
import java.util.stream.Collectors;

/** Shared evaluation rules for query responses and persisted progress. */
final class CriteriaProgressProjection {
    private CriteriaProgressProjection() {}

    static List<CriteriaProgressItemDto> build(List<CriteriaTreeNodeDto> tree,
            List<StudentCriteriaTransactionJpaEntity> transactions) {
        Map<Long, List<StudentCriteriaTransactionJpaEntity>> byCriteria = transactions.stream().filter(tx -> Boolean.TRUE.equals(tx.getIsActive()))
                .collect(Collectors.groupingBy(tx -> tx.getCriteria().getId()));
        return buildNodes(tree, byCriteria);
    }

    private static List<CriteriaProgressItemDto> buildNodes(List<CriteriaTreeNodeDto> tree,
            Map<Long, List<StudentCriteriaTransactionJpaEntity>> byCriteria) {
        if (tree == null) return List.of();
        return tree.stream().sorted(Comparator
                .comparing(CriteriaTreeNodeDto::childrenOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CriteriaTreeNodeDto::id))
                .map(node -> buildNode(node, byCriteria)).toList();
    }

    private static CriteriaProgressItemDto buildNode(CriteriaTreeNodeDto node,
            Map<Long, List<StudentCriteriaTransactionJpaEntity>> byCriteria) {
        var children = buildNodes(node.children(), byCriteria);
        var records = byCriteria.getOrDefault(node.id(), List.of()).stream()
                .sorted(Comparator.comparing(StudentCriteriaTransactionJpaEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StudentCriteriaTransactionJpaEntity::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        var manual = records.stream().filter(tx -> "MANUAL".equals(tx.getCompletionSource())).findFirst().orElse(null);
        var system = records.stream().filter(tx -> "SYSTEM".equals(tx.getCompletionSource())
                || "EVENT_CHECKOUT".equals(tx.getCompletionSource())).toList();
        int systemOccurrences = (int) system.stream().map(tx -> tx.getEvent() != null
                ? "event:" + tx.getEvent().getId() : "system:" + tx.getId()).distinct().count();
        int manualOccurrences = (int) records.stream().filter(tx -> "MANUAL".equals(tx.getCompletionSource())).count();
        int occurrences = systemOccurrences + manualOccurrences;
        int required = node.requiredOccurrences() == null ? 1 : Math.max(1, node.requiredOccurrences());
        int completedChildren = (int) children.stream().filter(CriteriaProgressItemDto::isCompleted).count();
        int requiredChildren = node.requiredChildren() == null ? 1 : node.requiredChildren();
        boolean leaf = children.isEmpty();
        boolean systemCompleted = leaf ? systemOccurrences >= required
                : children.stream().filter(CriteriaProgressItemDto::isSystemCompleted).count() >= requiredChildren;
        boolean completed = leaf ? occurrences >= required
                : completedChildren >= requiredChildren;
        var latestSystem = system.isEmpty() ? null : system.get(0);
        String source = null;
        if (leaf && systemCompleted) source = latestSystem.getCompletionSource();
        else if (leaf && completed && manual != null) source = "MANUAL";
        String eventName = system.stream().filter(tx -> tx.getEvent() != null)
                .findFirst().map(tx -> tx.getEvent().getName()).orElse(null);
        return new CriteriaProgressItemDto(node.id(), node.name(), node.note(), node.scope(), node.level(),
                node.childrenOrder(), leaf ? node.requiredChildren() : Integer.valueOf(requiredChildren), completedChildren, required, occurrences,
                completed, children.size(), leaf, !system.isEmpty(), systemCompleted, manual != null,
                manual == null ? null : manual.getNote(), manual == null ? null : manual.getId(),
                source, eventName, leaf ? Math.min(100, occurrences * 100 / required)
                        : requiredChildren == 0 ? 0 : Math.min(100, completedChildren * 100 / requiredChildren), children);
    }
}
