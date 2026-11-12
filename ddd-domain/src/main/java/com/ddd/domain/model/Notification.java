package com.ddd.domain.model;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseModel {
    private NotificationTypeEnum type;
    private String message;
    private NotificationStatusEnum status = NotificationStatusEnum.UNREAD;
    private String iconUrl;
    private String url;
    private Long userId;
    private Long fanpageId;

    public Notification(NotificationTypeEnum type, String message, NotificationStatusEnum status, String iconUrl, String url, Long userId) {
        this(type, message, status, iconUrl, url, userId, null);
    }

    public void markAsRead() {
        this.status = NotificationStatusEnum.READ;
    }

    public boolean isUnread() {
        return this.status == NotificationStatusEnum.UNREAD;
    }

    public boolean isFanpageNotification() {
        return this.fanpageId != null;
    }

    public boolean isUserNotification() {
        return this.userId != null;
    }
}
