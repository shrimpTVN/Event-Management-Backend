package com.ddd.application.service.fanpage.repository;

import com.ddd.application.dto.fanpage.FanpageMemberDto;

import java.util.List;

/**
 * Application query port for retrieving fanpage members.
 * Implemented in infrastructure layer to uphold DDD dependency inversion.
 */
public interface FanpageMemberQueryRepository {
    List<FanpageMemberDto> findMembersByFanpageId(Long fanpageId);
}
