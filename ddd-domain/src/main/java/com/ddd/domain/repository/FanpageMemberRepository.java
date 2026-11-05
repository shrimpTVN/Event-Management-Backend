package com.ddd.domain.repository;

import com.ddd.domain.model.FanpageMember;

import java.util.List;
import java.util.Optional;

public interface FanpageMemberRepository {
    Optional<FanpageMember> findById(Long fanpageId, Long userId);

    FanpageMember save(FanpageMember fanpageMember);

    boolean isAlreadyHasFanpage(Long userId);

    boolean isAdminOfFanpage(Long fanpageId, Long userId);

    void removeFanpageMember(Long fanpageId, Long userId);

    List<Long> findFanpageIdsByUserIdAndRole(Long userId, String role);

    void deactivateAllMembersByFanpageId(Long fanpageId);

    boolean isActiveMember(Long fanpageId, Long memberId);

    default boolean isFanpageAdmin(Long userId, Long fanpageId) {
        return isAdminOfFanpage(fanpageId, userId);
    }

    FanpageMember findByFanpageIdAndUserId(Long fanpageId, Long userId);
}
