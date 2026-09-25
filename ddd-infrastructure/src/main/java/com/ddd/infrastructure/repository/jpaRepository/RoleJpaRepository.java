package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByName(String name);

    List<RoleJpaEntity> findAllByActive(boolean isActive);
}
