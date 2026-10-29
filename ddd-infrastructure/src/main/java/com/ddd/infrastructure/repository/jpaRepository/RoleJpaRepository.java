package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByName(String name);

    Set<RoleJpaEntity> findByNameIn(Collection<String> names);

    List<RoleJpaEntity> findAllByIsActive(boolean isActive);
}
