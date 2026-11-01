package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageInfoDto;
import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.dto.fanpage.FanpageSearchCriteriaDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.specification.FanpageSpecification;
import com.ddd.application.dto.fanpage.FanpageFollowCountDto;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
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
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationDtoMapper registrationDtoMapper;
    private final FanpageJpaRepository fanpageJpaRepository;
    private final FanpageDtoMapper fanpageDtoMapper;
    private final FanpageRepository fanpageRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final UserRepository userRepository;
    private final FollowingJpaRepository followingJpaRepository;

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

    @Override
    public FanpageInfoDto getFanpageById(Long fanpageId, String currentUserEmail, boolean isAdmin) {
        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        if (isAdmin) {
            return fanpageDtoMapper.toInfoDto(fanpage);
        }

        // If not admin, check if user is fanpage admin of this fanpage
        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            User user = userRepository.findByEmail(currentUserEmail);
            if (user != null && fanpageMemberRepository.isAdminOfFanpage(fanpageId, user.getId())) {
                return fanpageDtoMapper.toInfoDto(fanpage);
            }
        }

        // For regular users, fanpage must be in ACTIVE status
        if (!FanpageStatusEnum.ACTIVE.name().equalsIgnoreCase(fanpage.getStatus())) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }

        return fanpageDtoMapper.toInfoDto(fanpage);
    }

    @Override
    public List<FanpageInfoDto> getMyManagedFanpages(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        List<Long> managedFanpageIds = fanpageMemberRepository.findFanpageIdsByUserIdAndRole(user.getId(), FanpageRoleEnum.ADMIN.name());
        if (managedFanpageIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Fanpage> fanpages = fanpageRepository.findAllByIdsAndIsActive(managedFanpageIds, true);
        return fanpages.stream().map(fanpageDtoMapper::toInfoDto).toList();
    }

    @Override
    public FanpageFollowCountDto getFollowerCount(Long fanpageId, String currentUserEmail) {
        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        long count = followingJpaRepository.countByFanpage_IdAndFolStatusAndIsActive(
                fanpageId,
                FollowingStatusEnum.FOLLOWING,
                true
        );

        Boolean isFollowing = null;
        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            User user = userRepository.findByEmail(currentUserEmail);
            if (user != null) {
                isFollowing = followingJpaRepository.existsByFanpage_IdAndUser_IdAndFolStatusAndIsActive(
                        fanpageId,
                        user.getId(),
                        FollowingStatusEnum.FOLLOWING,
                        true
                );
            }
        }

        return new FanpageFollowCountDto(fanpageId, count, isFollowing);
    }

    @Override
    public Page<FanpageInfoDto> getFollowingFanpages(String currentUserEmail, Pageable pageable) {
        User user = userRepository.findByEmail(currentUserEmail);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Page<FanpageJpaEntity> fanpages = followingJpaRepository.findFollowingFanpagesByUserIdAndStatus(
                user.getId(),
                FollowingStatusEnum.FOLLOWING,
                pageable
        );

        return fanpages.map(fanpageDtoMapper::toInfoDto);
    }
}
