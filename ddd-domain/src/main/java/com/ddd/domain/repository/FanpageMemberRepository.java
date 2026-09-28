package com.ddd.domain.repository;

import com.ddd.domain.model.FanpageMember;

import java.util.Optional;

public interface FanpageMemberRepository {
    Optional<FanpageMember> findById(Long fanpageId, Long userId);

    FanpageMember save(FanpageMember fanpageMember);

    boolean isAlreadyHasFanpage(Long userId);
}
