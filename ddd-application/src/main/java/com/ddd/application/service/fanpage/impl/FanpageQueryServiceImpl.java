package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.service.fanpage.repository.FanpageMemberQueryRepository;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageMemberDtoMapper fanpageMemberDtoMapper;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        List<FanpageMemberJpaEntity> members = fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
        return members.stream().map(fanpageMemberDtoMapper::toDto).collect(java.util.stream.Collectors.toList());
    }
}
