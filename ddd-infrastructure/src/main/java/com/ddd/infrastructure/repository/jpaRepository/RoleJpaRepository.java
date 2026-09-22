package com.ddd.infrastructure.repository.jpaRepository;


import com.ddd.infrastructure.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {
}
