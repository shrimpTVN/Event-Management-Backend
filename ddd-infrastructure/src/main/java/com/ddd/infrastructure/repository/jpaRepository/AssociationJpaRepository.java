package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.AssociationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssociationJpaRepository  extends JpaRepository<AssociationJpaEntity, Long> {
    boolean existsById(Long id);
}
