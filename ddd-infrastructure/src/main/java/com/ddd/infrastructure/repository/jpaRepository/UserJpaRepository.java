package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long>, JpaSpecificationExecutor<UserJpaEntity> {
    @EntityGraph(attributePaths = {"roles"})
    UserJpaEntity findByEmail(String email);

    Boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Page<UserJpaEntity> findAllByIsActive(boolean isActive, Pageable pageable);

    @EntityGraph(attributePaths = {
            "studentProfile",
            "studentProfile.major",
            "studentProfile.major.school",
            "fanpageAdminProfile"
    })
    @Query("SELECT u FROM UserJpaEntity u WHERE u.isActive = :isActive")
    Page<UserJpaEntity> findAllAdminUsersByIsActive(@Param("isActive") boolean isActive, Pageable pageable);

    @Query("SELECT DISTINCT u FROM UserJpaEntity u LEFT JOIN FETCH u.roles WHERE u.id IN :userIds")
    List<UserJpaEntity> findUsersWithRolesByIdIn(@Param("userIds") Collection<Long> userIds);

    @EntityGraph(attributePaths = {"roles", "fanpageAdminProfile"})
    @Query("SELECT u FROM UserJpaEntity u WHERE u.id = :id")
    Optional<UserJpaEntity> findAdminProfileById(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserJpaEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    Page<UserJpaEntity> findAll(Specification<UserJpaEntity> spec, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserJpaEntity u WHERE u.id = :id")
    Optional<UserJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
