package com.ddd.infrastructure.repository.jpaRepository;

import com.ddd.domain.enums.InvatitationStatusEnum;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FanpageMemberInvitationJpaRepository extends JpaRepository<FanpageMemberInvitationJpaEntity, Long> {

    List<FanpageMemberInvitationJpaEntity> findByUserId(Long userId);

    List<FanpageMemberInvitationJpaEntity> findByUserIdAndStatus(Long userId, InvatitationStatusEnum status);


    Page<FanpageMemberInvitationJpaEntity> findByFanpageIdAndStatus(Long fanpageId, InvatitationStatusEnum status, Pageable pageable
    );
}
