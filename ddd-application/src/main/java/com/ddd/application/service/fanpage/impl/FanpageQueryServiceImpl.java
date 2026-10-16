package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageMemberDtoMapper fanpageMemberDtoMapper;
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationDtoMapper registrationDtoMapper;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        List<FanpageMemberJpaEntity> members = fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
        return members.stream().map(fanpageMemberDtoMapper::toDto).collect(java.util.stream.Collectors.toList());
    }


}
