package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface AdminUserSpecificationRepository {
    Page<UserJpaEntity> findAdminUsers(Specification<UserJpaEntity> spec, Pageable pageable);
}
