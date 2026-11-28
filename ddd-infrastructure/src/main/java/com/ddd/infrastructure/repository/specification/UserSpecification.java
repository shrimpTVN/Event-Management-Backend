package com.ddd.infrastructure.repository.specification;

import com.ddd.infrastructure.entity.MajorJpaEntity;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.SchoolJpaEntity;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.StudentProfileJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<UserJpaEntity> adminUsersWithFilter(
            Boolean isActive,
            String role,
            String keyword
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            Join<UserJpaEntity, StudentProfileJpaEntity> studentProfile =
                    root.join("studentProfile", JoinType.LEFT);
            Join<StudentProfileJpaEntity, MajorJpaEntity> major =
                    studentProfile.join("major", JoinType.LEFT);
            Join<MajorJpaEntity, SchoolJpaEntity> school =
                    major.join("school", JoinType.LEFT);
            Join<UserJpaEntity, FanpageAdminProfileJpaEntity> staffProfile =
                    root.join("fanpageAdminProfile", JoinType.LEFT);

            if (role != null && !role.isBlank()) {
                String normalized = role.trim().toUpperCase().replaceFirst("^ROLE_", "");
                Predicate isSuperAdmin = hasAnyRole(root, query, cb, "ADMIN", "SUPERADMIN", "SUPER_ADMIN");
                Predicate isStudent = cb.and(
                        cb.not(isSuperAdmin),
                        cb.or(
                                cb.isNotNull(studentProfile.get("id")),
                                hasAnyRole(root, query, cb, "STUDENT")
                        )
                );

                switch (normalized) {
                    case "ADMIN", "SUPERADMIN", "SUPER_ADMIN" -> predicates.add(isSuperAdmin);
                    case "STUDENT" -> predicates.add(isStudent);
                    case "FANPAGE_ADMIN", "STAFF", "ORGANIZER" -> predicates.add(cb.and(
                            cb.not(isSuperAdmin),
                            cb.not(isStudent),
                            cb.or(
                                    cb.isNotNull(staffProfile.get("id")),
                                    hasAnyRole(root, query, cb, "FANPAGE_ADMIN", "STAFF", "ORGANIZER")
                            )
                    ));
                    default -> predicates.add(hasAnyRole(root, query, cb, normalized));
                }
            }

            if (keyword != null && !keyword.isBlank()) {
                String escaped = keyword.trim().toLowerCase()
                        .replace("\\", "\\\\")
                        .replace("%", "\\%")
                        .replace("_", "\\_");
                String pattern = "%" + escaped + "%";

                Expression<String> firstLower = cb.lower(cb.coalesce(root.get("firstName"), ""));
                Expression<String> lastLower = cb.lower(cb.coalesce(root.get("lastName"), ""));
                Expression<String> firstLast = cb.concat(cb.concat(firstLower, " "), lastLower);
                Expression<String> lastFirst = cb.concat(cb.concat(lastLower, " "), firstLower);

                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("email"), "")), pattern, '\\'),
                        cb.like(firstLower, pattern, '\\'),
                        cb.like(lastLower, pattern, '\\'),
                        cb.like(firstLast, pattern, '\\'),
                        cb.like(lastFirst, pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(studentProfile.get("studentId"), "")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(staffProfile.get("staffId"), "")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(school.get("name"), "")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(major.get("name"), "")), pattern, '\\')
                ));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static Predicate hasAnyRole(
            Root<UserJpaEntity> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            String... roleNames
    ) {
        List<String> upperWithPrefixes = new ArrayList<>();
        for (String name : Arrays.asList(roleNames)) {
            String upper = name.toUpperCase();
            upperWithPrefixes.add(upper);
            upperWithPrefixes.add("ROLE_" + upper);
        }
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<UserJpaEntity> subUser = subquery.from(UserJpaEntity.class);
        Join<UserJpaEntity, RoleJpaEntity> roleJoin = subUser.join("roles");
        subquery.select(cb.literal(1L)).where(
                cb.equal(subUser.get("id"), root.get("id")),
                cb.upper(roleJoin.get("name")).in(upperWithPrefixes)
        );
        return cb.exists(subquery);
    }

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
