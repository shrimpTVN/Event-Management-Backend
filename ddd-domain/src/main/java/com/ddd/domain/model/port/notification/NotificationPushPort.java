package com.ddd.domain.model.port.notification;

import com.ddd.domain.model.Notification;

public interface NotificationPushPort {
    void pushToUser(Long userId, Notification notification);

    void pushToFanpage(Long fanpageId, Notification notification);
}
