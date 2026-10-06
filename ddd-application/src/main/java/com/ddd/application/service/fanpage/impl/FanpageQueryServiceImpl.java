package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageDtoMapper fanpageDtoMapper;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        List<FanpageMemberJpaEntity> fanpageMembers = fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
        return fanpageMembers.stream().map(fanpageDtoMapper::toMemberDto).toList();
    }
}
