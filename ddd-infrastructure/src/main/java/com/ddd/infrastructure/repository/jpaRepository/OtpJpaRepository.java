package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.OtpJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpJpaRepository extends JpaRepository<OtpJpaEntity, Long> {
    //get otp by email and isUsed false and order by createdAt desc
    Optional<OtpJpaEntity> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    Optional<OtpJpaEntity> findTopByEmailOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("UPDATE OtpJpaEntity o SET o.isUsed = true WHERE o.id = :id")
    void markOtpUsed(@Param("id") Long id);
}
