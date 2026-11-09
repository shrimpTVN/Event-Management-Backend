package com.ddd.application.service.notification;

import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.domain.enums.NotificationStatusEnum;
import org.springframework.data.domain.Page;

public interface NotificationQueryService {
    Page<NotificationInfoDto> getMyNotifications(int page, int size, String sortBy, String sortDir, NotificationStatusEnum status);

    long getMyUnreadCount();
}
