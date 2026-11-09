package com.ddd.domain.model;

import com.ddd.domain.enums.NotificationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseModel {
    private String title;
    private String message;
    private NotificationStatusEnum status = NotificationStatusEnum.UNREAD;
    private String iconUrl;
    private String url;
    private Long userId;

    public void markAsRead() {
        this.status = NotificationStatusEnum.READ;
    }

    public boolean isUnread() {
        return this.status == NotificationStatusEnum.UNREAD;
    }
}
