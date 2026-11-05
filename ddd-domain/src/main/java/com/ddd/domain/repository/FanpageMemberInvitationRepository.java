package com.ddd.domain.repository;

import com.ddd.domain.model.FanpageMemberInvitation;

import java.util.Optional;

public interface FanpageMemberInvitationRepository {
    FanpageMemberInvitation save(FanpageMemberInvitation invitation);

    Optional<FanpageMemberInvitation> findById(Long id);
}
