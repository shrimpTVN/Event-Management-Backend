package com.ddd.infrastructure.repository.specification;

import com.ddd.infrastructure.entity.FanpageJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class FanpageSpecification {

    private FanpageSpecification() {}

    /**
     * Builds dynamic JPA Specification for searching and filtering fanpages.
     *
     * @param keyword search term matched case-insensitively against name, description, orgName, and parentOrg
     * @param status Fanpage status (PENDING, ACTIVE, BANNED)
     * @param orgType Fanpage organization type
     * @param isActive Soft-delete active flag
     * @return Specification instance for JPA query execution
     */
    public static Specification<FanpageJpaEntity> withFilter(String keyword, String status,
                                                             String orgType, Boolean isActive) {
        return withFilter(keyword, status, null, orgType, isActive);
    }

    public static Specification<FanpageJpaEntity> withFilter(String keyword, String status,
                                                             List<String> statuses,
                                                             String orgType, Boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(cb.coalesce(root.get("name"), "")), pattern);
                Predicate descMatch = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), pattern);
                Predicate orgNameMatch = cb.like(cb.lower(cb.coalesce(root.get("orgName"), "")), pattern);
                Predicate parentOrgMatch = cb.like(cb.lower(cb.coalesce(root.get("parentOrg"), "")), pattern);
                predicates.add(cb.or(nameMatch, descMatch, orgNameMatch, parentOrgMatch));
            }

            if (statuses != null && !statuses.isEmpty()) {
                List<String> normalizedStatuses = statuses.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase())
                        .toList();
                if (!normalizedStatuses.isEmpty()) {
                    predicates.add(cb.upper(root.get("status")).in(normalizedStatuses));
                }
            } else if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            if (orgType != null && !orgType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("orgType")), orgType.trim().toUpperCase()));
            }

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
