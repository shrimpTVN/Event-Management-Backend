package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    @EntityGraph(attributePaths = {"roles"})
    UserJpaEntity findByEmail(String email);

    Boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Page<UserJpaEntity> findAllByIsActive(boolean isActive, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserJpaEntity> findById(Long id);
}
