package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.*;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import com.ddd.infrastructure.repository.specification.FanpageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageQueryServiceImpl implements FanpageQueryService {
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final FanpageMemberDtoMapper fanpageMemberDtoMapper;
    private final FanpageJpaRepository fanpageJpaRepository;
    private final FanpageDtoMapper fanpageDtoMapper;
    private final FanpageRepository fanpageRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FollowingRepository followingRepository;
    private final FollowingJpaRepository followingJpaRepository;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        if (fanpageRepository.findByIdAndIsActive(fanpageId, true).isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }
        List<FanpageMemberJpaEntity> members = fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
        return members.stream().map(fanpageMemberDtoMapper::toDto).toList();
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

    @Override
    public FanpageInfoDto getFanpageById(Long fanpageId) {
        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        if (securityContextPort.hasRole("ADMIN")) {
            return fanpageDtoMapper.toInfoDto(fanpage);
        }

        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId != null && fanpageMemberRepository.isAdminOfFanpage(fanpageId, currentUserId)) {
            return fanpageDtoMapper.toInfoDto(fanpage);
        }

        if (!FanpageStatusEnum.ACTIVE.name().equalsIgnoreCase(fanpage.getStatus())) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }

        return fanpageDtoMapper.toInfoDto(fanpage);
    }

    @Override
    public List<ManagedFanpageInfoDto> getMyManagedFanpages() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        List<FanpageMemberJpaEntity> managedFanpages = fanpageMemberJpaRepository
                .findAllByUserIdAndIsActive(currentUserId, true);

        return managedFanpages.stream().map(fanpageDtoMapper::toManagedInfoDto).toList();
    }

    @Override
    public FanpageFollowCountDto getFollowerCount(Long fanpageId) {
        if (fanpageRepository.findByIdAndIsActive(fanpageId, true).isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }

        long count = followingRepository.countByFanpageIdAndFolStatus(
                fanpageId,
                FollowingStatusEnum.FOLLOWING
        );

        Boolean isFollowing = null;
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId != null) {
            isFollowing = followingRepository.existsByFanpageIdAndUserIdAndFolStatus(
                    fanpageId,
                    currentUserId,
                    FollowingStatusEnum.FOLLOWING
            );
        }

        return new FanpageFollowCountDto(fanpageId, count, isFollowing);
    }

    @Override
    public Page<FanpageInfoDto> getFollowingFanpages(Pageable pageable) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        Page<FanpageJpaEntity> fanpages = followingJpaRepository.findFollowingFanpagesByUserIdAndStatus(
                currentUserId,
                FollowingStatusEnum.FOLLOWING,
                pageable
        );

        return fanpages.map(fanpageDtoMapper::toInfoDto);
    }
}
