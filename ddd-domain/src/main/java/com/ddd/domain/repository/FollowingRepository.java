package com.ddd.domain.repository;

import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.model.Following;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FollowingRepository {
    Optional<Following> findByFanpageIdAndUserId(Long fanpageId, Long userId);
    Following save(Following following);
    long countByFanpageIdAndFolStatus(Long fanpageId, FollowingStatusEnum folStatus);
    boolean existsByFanpageIdAndUserIdAndFolStatus(Long fanpageId, Long userId, FollowingStatusEnum folStatus);

    List<Following> findByFanpageIdAndFolStatus(Long id, FollowingStatusEnum followingStatusEnum);
}
