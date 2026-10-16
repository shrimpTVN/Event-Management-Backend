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

    public static Specification<EventJpaEntity> withFilter(String keyword, String status,
                                                           Long eventTypeId, Long semesterId,
                                                           Long criteriaId, Long pointCategoryId,
                                                           Instant dateHappenFrom, Instant dateHappenTo,
                                                           Boolean isActive) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String patten = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), patten);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), patten);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            //define subquery form EventPointJpaEntity to filter event by pointCategoryId
            if (pointCategoryId != null){
                Subquery<Long> subquery = query.subquery(Long.class);
                var eventPointRoot = subquery.from(EventPointJpaEntity.class);
                subquery.select(eventPointRoot.get("event").get("id"))
                        .where(cb.equal(eventPointRoot.get("pointCategory").get("id"), pointCategoryId));
                predicates.add(root.get("id").in(subquery));
            }

            if (eventTypeId != null) {
                Predicate eventTypeMatch = cb.equal(root.get("eventType").get("id"), eventTypeId);
                predicates.add(eventTypeMatch);
            }

            if (semesterId != null){
                Predicate semesterMatch = cb.equal(root.get("semester").get("id"), semesterId);
                predicates.add(semesterMatch);
            }

            if (criteriaId != null){
                Predicate criteriaMatch = cb.equal(root.get("criteria").get("id"), criteriaId);
                predicates.add(criteriaMatch);
            }

            if (status !=null && !status.trim().isEmpty()){
                Predicate statusMatch = cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase());
                predicates.add(statusMatch);
            }

            if (dateHappenFrom != null && dateHappenTo != null){
                Predicate dateHappenMatch = cb.between(root.get("dataHappen"), dateHappenFrom, dateHappenTo);
                predicates.add(dateHappenMatch);
            }

            if (isActive != null){
                Predicate isActiveMatch = cb.equal(root.get("isActive"), isActive);
                predicates.add(isActiveMatch);
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
