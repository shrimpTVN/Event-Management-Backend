package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    UserJpaEntity findByEmail(String email);

    Boolean existsByEmail(String email);

}
