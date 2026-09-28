package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FanpageAdminProfileJpaRepository  extends JpaRepository<FanpageAdminProfileJpaEntity, Long> {
    boolean existsByStaffId(String s);
}
