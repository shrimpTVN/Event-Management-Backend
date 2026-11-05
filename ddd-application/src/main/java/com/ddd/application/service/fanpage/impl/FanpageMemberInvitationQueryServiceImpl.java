package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageMemberInvitationInfoDto;
import com.ddd.application.mapper.FanpageMemberInvitationDtoMapper;
import com.ddd.application.service.fanpage.FanpageMemberInvitationQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.enums.InvatitationStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.FanpageMemberInvitationJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberInvitationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanpageMemberInvitationQueryServiceImpl implements FanpageMemberInvitationQueryService {

    private final FanpageMemberInvitationJpaRepository invitationJpaRepository;
    private final UserRepository userRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FanpageMemberInvitationDtoMapper fanpageMemberInvitationDtoMapper;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<FanpageMemberInvitationInfoDto> getAllFanpageMemberInvitationsForUser(String userEmail) {
        Long userId = securityContextPort.getCurrentUserId();

        List<FanpageMemberInvitationJpaEntity> entities = invitationJpaRepository.findByUserId(userId);
        return entities.stream()
                .map(fanpageMemberInvitationDtoMapper::toInfoDto)
                .toList();
    }

    @Override
    public Page<FanpageMemberInvitationInfoDto> getAllFanpageMemberInvitationsForFanpage(
            Long fanpageId,
            InvatitationStatusEnum status,
            int page,
            int size,
            String sortBy,
            String sortDir,
            String adminEmail) {

        Long adminId = securityContextPort.getCurrentUserId();

        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, adminId)) {
            throw new AuthException("You are not an admin of this fanpage");
        }

        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<FanpageMemberInvitationJpaEntity> pageResult = invitationJpaRepository
                .findByFanpageIdAndStatus(fanpageId, status, pageable);

        return pageResult.map(fanpageMemberInvitationDtoMapper::toInfoDto);
    }
}
