package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.statistic.AlertAbsentStudentsDto;
import com.ddd.application.service.fanpage.FanpageStatisticCommandService;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.domain.enums.NotificationTypeEnum;
import com.ddd.domain.exception.ForbiddenException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FanpageStatisticCommandServiceImpl implements FanpageStatisticCommandService {

    private final FanpageRepository fanpageRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final SecurityContextPort securityContextPort;
    private final NotificationCommandService notificationCommandService;

    @Override
    public void alertAbsentStudents(Long fanpageId, AlertAbsentStudentsDto dto) {
        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        assertCallerIsAdminOfFanpage(fanpageId);

        if (dto.studentUserIds() == null || dto.studentUserIds().isEmpty()) {
            return;
        }

        String warningMessage = (dto.message() != null && !dto.message().isBlank())
                ? dto.message().trim()
                : String.format("Warning from %s: You have been marked absent from multiple club events this semester.", fanpage.getName());

        String fanpageUrl = "/fanpages/" + fanpageId;
        String iconUrl = fanpage.getAvatarUrl();

        List<CreateNotificationDto> notifications = dto.studentUserIds().stream()
                .map(userId -> CreateNotificationDto.forUser(
                        userId,
                        NotificationTypeEnum.EVENT_ABSENCE_ALERT,
                        warningMessage,
                        iconUrl,
                        fanpageUrl
                ))
                .toList();

        notificationCommandService.createNotifications(notifications);
        log.info("Sent absence alert notifications to {} students for fanpage: {}", dto.studentUserIds().size(), fanpageId);
    }

    private void assertCallerIsAdminOfFanpage(Long fanpageId) {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, currentUserId)) {
            throw new ForbiddenException("You do not have permission to send alerts for this fanpage. Only the fanpage admin can access this action.");
        }
    }
}
