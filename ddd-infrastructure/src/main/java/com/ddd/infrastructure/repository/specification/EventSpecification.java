package com.ddd.infrastructure.repository.specification;

import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.EventPointJpaEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class EventSpecification {
    private EventSpecification() {}

    public static Specification<EventJpaEntity> withFilter(
            String keyword,
            String status,
            List<Long> eventTypeIds,
            Long semesterId,
            List<Long> criteriaIds,
            List<Long> pointCategoryIds,
            Instant dateHappenFrom,
            Instant dateHappenTo,
            Boolean isActive) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.trim().toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                String pattern = "%" + escaped + "%";

                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern, '\\');
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern, '\\');
                predicates.add(cb.or(nameMatch, descMatch));
            }

            // Correlated EXISTS subquery for pointCategoryIds (safe for Pageable count queries)
            if (pointCategoryIds != null && !pointCategoryIds.isEmpty()) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                var eventPointRoot = subquery.from(EventPointJpaEntity.class);
                subquery.select(cb.literal(1))
                        .where(
                                cb.equal(eventPointRoot.get("event"), root),
                                eventPointRoot.get("pointCategory").get("id").in(pointCategoryIds)
                        );
                predicates.add(cb.exists(subquery));
            }

            if (eventTypeIds != null && !eventTypeIds.isEmpty()) {
                predicates.add(root.get("eventType").get("id").in(eventTypeIds));
            }

            if (semesterId != null) {
                predicates.add(cb.equal(root.get("semester").get("id"), semesterId));
            }

            if (criteriaIds != null && !criteriaIds.isEmpty()) {
                predicates.add(root.get("criteria").get("id").in(criteriaIds));
            }

            if (status != null && !status.isBlank()) {
                // Note: If 'status' on EventJpaEntity is an Enum, convert to Enum instead of using cb.upper()
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (dateHappenFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateHappen"), dateHappenFrom));
            }

            if (dateHappenTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateHappen"), dateHappenTo));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}