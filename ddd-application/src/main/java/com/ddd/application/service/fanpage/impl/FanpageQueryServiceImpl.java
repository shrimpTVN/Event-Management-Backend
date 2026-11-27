package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.*;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.application.service.fanpage.FanpageQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.enums.FollowingStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
import com.ddd.infrastructure.entity.FanpageJpaEntity;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import com.ddd.infrastructure.repository.specification.FanpageSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final EventJpaRepository eventJpaRepository;
    private final RegistrationJpaRepository registrationJpaRepository;

    @Override
    public List<FanpageMemberDto> getMembersOfFanpage(Long fanpageId) {
        boolean isAdmin = securityContextPort.hasRole("ADMIN");
        var fanpageOpt = isAdmin
                ? fanpageRepository.findById(fanpageId)
                : fanpageRepository.findByIdAndIsActive(fanpageId, true);

        if (fanpageOpt.isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }
        List<FanpageMemberJpaEntity> members = isAdmin
                ? fanpageMemberJpaRepository.findAllByFanpageId(fanpageId)
                : fanpageMemberJpaRepository.findAllByFanpageIdAndIsActive(fanpageId, true);
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
    public Page<FanpageInfoDto> searchAndFilterFanpagesForAdmin(FanpageSearchCriteriaDto criteria) {
        Pageable pageable = ServiceUtil.createPageable(criteria.page(), criteria.size(), criteria.sortBy(), criteria.sortDir());
        Specification<FanpageJpaEntity> spec = FanpageSpecification.withFilter(
                criteria.keyword(),
                criteria.status(),
                criteria.orgType(),
                criteria.isActive()
        );
        Page<FanpageJpaEntity> page = fanpageJpaRepository.findAll(spec, pageable);
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> fanpageIds = page.getContent().stream()
                .map(FanpageJpaEntity::getId)
                .toList();

        Map<Long, Long> followerCounts = toCountsMap(
                followingJpaRepository.countFollowersByFanpageIds(fanpageIds, FollowingStatusEnum.FOLLOWING));
        Map<Long, Long> eventCounts = toCountsMap(
                eventJpaRepository.countActiveEventsByFanpageIds(fanpageIds));
        Map<Long, FanpageMemberJpaEntity> adminByFanpageId = fanpageMemberJpaRepository
                .findActiveMembersByFanpageIdsAndRole(fanpageIds, FanpageRoleEnum.ADMIN.name())
                .stream()
                .collect(Collectors.toMap(
                        member -> member.getId().getFanpageId(),
                        member -> member,
                        (existing, replacement) -> existing
                ));

        return page.map(entity -> enrichAdminFanpageInfo(
                fanpageDtoMapper.toInfoDto(entity),
                followerCounts.getOrDefault(entity.getId(), 0L),
                eventCounts.getOrDefault(entity.getId(), 0L),
                adminByFanpageId.get(entity.getId())
        ));
    }

    private static Map<Long, Long> toCountsMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
            }
        }
        return result;
    }

    private static FanpageInfoDto enrichAdminFanpageInfo(
            FanpageInfoDto base,
            Long followerCount,
            Long eventsCount,
            FanpageMemberJpaEntity adminMember
    ) {
        UserJpaEntity adminUser = adminMember != null ? adminMember.getUser() : null;
        Long repUserId = adminUser != null ? adminUser.getId() : null;
        String repName = adminUser != null
                ? Stream.of(adminUser.getFirstName(), adminUser.getLastName())
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(" "))
                : null;
        if (repName != null && repName.isEmpty()) {
            repName = null;
        }
        String repEmail = adminUser != null ? adminUser.getEmail() : null;
        String repCode = null;
        String repPhone = null;
        if (adminUser != null) {
            if (adminUser.getFanpageAdminProfile() != null) {
                repCode = adminUser.getFanpageAdminProfile().getStaffId();
                repPhone = adminUser.getFanpageAdminProfile().getPhoneNumber();
            } else if (adminUser.getStudentProfile() != null) {
                repCode = adminUser.getStudentProfile().getStudentId();
            }
        }
        return new FanpageInfoDto(
                base.id(),
                base.name(),
                base.description(),
                base.avatarUrl(),
                base.orgType(),
                base.orgName(),
                base.parentOrg(),
                base.evidenceUrl(),
                base.helpContact(),
                base.helpPhone(),
                base.createdAt(),
                base.status(),
                base.isActive(),
                followerCount,
                eventsCount,
                repUserId,
                repName,
                repEmail,
                repCode,
                repPhone
        );
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

        return getManagedFanpagesByUserId(currentUserId);
    }

    @Override
    public List<ManagedFanpageInfoDto> getManagedFanpagesByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<FanpageMemberJpaEntity> managedFanpages = fanpageMemberJpaRepository
                .findAllByUserIdAndIsActive(userId, true);

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

    @Override
    public UpcomingEventReminderDto getUpcomingReminder(Long fanpageId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }

        boolean isAdmin = securityContextPort.hasRole("ADMIN");
        var fanpageOpt = isAdmin
                ? fanpageRepository.findById(fanpageId)
                : fanpageRepository.findByIdAndIsActive(fanpageId, true);
        if (fanpageOpt.isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }

        if (!isAdmin && !fanpageMemberRepository.isActiveMember(fanpageId, currentUserId)) {
            throw new AuthException("User is not an active member of this fanpage");
        }

        return eventJpaRepository
                .findFirstByFanpage_IdAndIsActiveTrueAndStatusAndDateHappenAfterOrderByDateHappenAsc(
                        fanpageId,
                        EventStatusEnum.PUBLISHED,
                        Instant.now()
                )
                .map(event -> {
                    long registrationsCount = registrationJpaRepository.countActiveRegistrationsByEventId(
                            event.getId(),
                            RegistrationStatusEnum.CANCELLED
                    );
                    return new UpcomingEventReminderDto(
                            event.getId(),
                            event.getName(),
                            event.getDateHappen(),
                            registrationsCount
                    );
                })
                .orElse(null);
    }
}
