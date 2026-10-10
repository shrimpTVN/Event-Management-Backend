package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.projection.FanpageMemberProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageDtoMapper fanpageDtoMapper;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        // Query members joined with student and fanpage admin profile details
        List<FanpageMemberProjection> fanpageMembers = fanpageMemberJpaRepository.findMembersWithProfileByFanpageId(fanpageId);
        return fanpageMembers.stream().map(fanpageDtoMapper::toMemberDto).toList();
    }
}
