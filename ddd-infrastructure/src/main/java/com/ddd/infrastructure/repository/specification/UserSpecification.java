package com.ddd.infrastructure.repository.specification;

import com.ddd.infrastructure.entity.UserJpaEntity;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<UserJpaEntity> searchForInvitation(String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always restrict to active users
            predicates.add(cb.equal(root.get("isActive"), true));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate firstNameMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastNameMatch = cb.like(cb.lower(root.get("lastName")), pattern);

                // Concatenated full name match: "firstName lastName"
                Expression<String> fullName = cb.concat(
                        cb.concat(cb.lower(root.get("firstName")), " "),
                        cb.lower(root.get("lastName"))
                );
                Predicate fullNameMatch = cb.like(fullName, pattern);

                predicates.add(cb.or(emailMatch, firstNameMatch, lastNameMatch, fullNameMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
