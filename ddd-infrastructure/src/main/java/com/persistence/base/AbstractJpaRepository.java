package com.persistence.base;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.Optional;

public abstract class AbstractJpaRepository<E, ID extends Serializable> {

    @PersistenceContext
    protected EntityManager entityManager;

    private final Class<E> entityClass;

    protected AbstractJpaRepository(Class<E> entityClass) {
        this.entityClass = entityClass;
    }

    public void save(E entity) {
        // Nếu entity đã có trong persistence context thì merge, chưa có thì persist
        if (entityManager.contains(entity)) {
            entityManager.merge(entity);
        } else {
            entityManager.persist(entity);
        }
    }

    public Optional<E> findById(ID id) {
        E entity = entityManager.find(entityClass, id);
        return Optional.ofNullable(entity);
    }

    public void delete(E entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }
}