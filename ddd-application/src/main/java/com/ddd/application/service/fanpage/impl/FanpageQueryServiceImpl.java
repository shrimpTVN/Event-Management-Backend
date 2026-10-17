package com.ddd.application.service.fanpage.impl;
import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.FanpageSearchCriteriaDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.specification.FanpageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageMemberDtoMapper fanpageMemberDtoMapper;
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationDtoMapper registrationDtoMapper;
    private final FanpageJpaRepository fanpageJpaRepository;
    private final FanpageDtoMapper fanpageDtoMapper;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        List<FanpageMemberJpaEntity> members = fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
        return members.stream().map(fanpageMemberDtoMapper::toDto).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Page<FanpageInfoDto> searchAndFilterFanpages(FanpageSearchCriteriaDto criteria) {
        Pageable pageable = ServiceUtil.createPageable(criteria.page(), criteria.size(), criteria.sortBy(), criteria.sortDir());
        Specification<FanpageJpaEntity> spec = FanpageSpecification.withFilter(
                criteria.keyword(),
                criteria.status(),
                criteria.orgType(),
                criteria.isActive()
        );
        return fanpageJpaRepository.findAll(spec, pageable).map(fanpageDtoMapper::toInfoDto);
    }


}
