package com.ddd.infrastructure.repository.jpaRepository;

import aj.org.objectweb.asm.commons.Remapper;
import com.ddd.infrastructure.entity.OtpJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpJpaRepository extends JpaRepository<OtpJpaEntity, Long> {
    //get otp by email and isUsed false and order by createdAt desc
    Optional<OtpJpaEntity> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    Optional<OtpJpaEntity> findTopByEmailOrderByCreatedAtDesc(String email);
}
