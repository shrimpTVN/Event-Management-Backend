package com.ddd.application.service.notification.impl;

import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.mapper.NotificationDtoMapper;
import com.ddd.application.service.notification.NotificationQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationDtoMapper notificationDtoMapper;
    private final SecurityContextPort securityContextPort;
    private final FanpageMemberRepository fanpageMemberRepository;

    @Override
    public Page<NotificationInfoDto> getMyNotifications(int page, int size, String sortBy, String sortDir, NotificationStatusEnum status) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);

        if (status != null) {
            return notificationRepository.findAllByUserIdAndStatus(currentUserId, status, pageable)
                    .map(notificationDtoMapper::toInfoDto);
        }

        return notificationRepository.findAllByUserId(currentUserId, pageable)
                .map(notificationDtoMapper::toInfoDto);
    }

    @Override
    public long getMyUnreadCount() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        return notificationRepository.countUnreadByUserId(currentUserId);
    }

    @Override
    public Page<NotificationInfoDto> getFanpageNotifications(Long fanpageId, int page, int size, String sortBy, String sortDir, NotificationStatusEnum status) {
        validateFanpageAdmin(fanpageId);

        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);

        if (status != null) {
            return notificationRepository.findAllByFanpageIdAndStatus(fanpageId, status, pageable)
                    .map(notificationDtoMapper::toInfoDto);
        }

        return notificationRepository.findAllByFanpageId(fanpageId, pageable)
                .map(notificationDtoMapper::toInfoDto);
    }

    @Override
    public long getFanpageUnreadCount(Long fanpageId) {
        validateFanpageAdmin(fanpageId);
        return notificationRepository.countUnreadByFanpageId(fanpageId);
    }

    private void validateFanpageAdmin(Long fanpageId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, currentUserId)) {
            throw new AuthException("You are not admin of the fanpage");
        }
    }
}
