package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.query.QueryUtils;

import java.util.List;

public class AdminUserSpecificationRepositoryImpl implements AdminUserSpecificationRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<UserJpaEntity> findAdminUsers(Specification<UserJpaEntity> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<UserJpaEntity> dataQuery = cb.createQuery(UserJpaEntity.class);
        Root<UserJpaEntity> root = dataQuery.from(UserJpaEntity.class);
        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, dataQuery, cb);
            if (predicate != null) {
                dataQuery.where(predicate);
            }
        }
        if (pageable.getSort().isSorted()) {
            dataQuery.orderBy(QueryUtils.toOrders(pageable.getSort(), root, cb));
        }

        EntityGraph<UserJpaEntity> graph = entityManager.createEntityGraph(UserJpaEntity.class);
        Subgraph<?> studentProfileGraph = graph.addSubgraph("studentProfile");
        Subgraph<?> majorGraph = studentProfileGraph.addSubgraph("major");
        majorGraph.addAttributeNodes("school");
        graph.addAttributeNodes("fanpageAdminProfile");

        TypedQuery<UserJpaEntity> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setHint("jakarta.persistence.loadgraph", graph);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        List<UserJpaEntity> content = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<UserJpaEntity> countRoot = countQuery.from(UserJpaEntity.class);
        countQuery.select(cb.count(countRoot));
        if (spec != null) {
            Predicate countPredicate = spec.toPredicate(countRoot, countQuery, cb);
            if (countPredicate != null) {
                countQuery.where(countPredicate);
            }
        }
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }
}
