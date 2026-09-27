package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.infrastructure.entity.SchoolJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolJpaRepository extends JpaRepository<SchoolJpaEntity, Long> {

}
