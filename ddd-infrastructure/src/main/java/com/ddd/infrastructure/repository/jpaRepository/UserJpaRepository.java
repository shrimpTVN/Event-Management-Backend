package com.ddd.infrastructure.repository.jpaRepository;

import aj.org.objectweb.asm.commons.Remapper;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    UserJpaEntity findByEmail(String email);

    Boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    @Query("SELECT u FROM UserJpaEntity u JOIN FETCH u.role r WHERE u.isActive = :isActive")
    Page<UserJpaEntity> findAllByIsActive(boolean isActive, Pageable pageable);
}
