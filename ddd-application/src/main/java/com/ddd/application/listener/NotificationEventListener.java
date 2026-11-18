package com.ddd.application.listener;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.domain.enums.NotificationTypeEnum;
import com.ddd.domain.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    // 1. Fanpage Registration: Approved
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFanpageApproved(FanpageApprovedEvent event) {
        notificationCommandService.createNotification(new CreateNotificationDto(
                event.userId(),
                NotificationTypeEnum.FANPAGE_APPROVED,
                "Your fanpage \"" + event.fanpageName() + "\" has been approved by the university admin!",
                event.fanpageAvatarUrl(),
                "" + event.fanpageId()
        ));
    }

    // 1. Fanpage Registration: Rejected / Banned
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFanpageRejected(FanpageRejectedEvent event) {
        notificationCommandService.createNotification(new CreateNotificationDto(
                event.userId(),
                NotificationTypeEnum.FANPAGE_REJECTED,
                "Your fanpage \"" + event.fanpageName() + "\" was rejected or suspended.",
                event.fanpageAvatarUrl(),
                "" + event.fanpageId()
        ));
    }

    // 2. Member Invited
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMemberInvited(FanpageMemberInvitedEvent event) {
        notificationCommandService.createNotification(new CreateNotificationDto(
                event.invitedUserId(),
                NotificationTypeEnum.FANPAGE_MEMBER_INVITED,
                event.inviterName() + " invited you to join \"" + event.fanpageName() + "\".",
                event.fanpageAvatarUrl(),
                "" + event.fanpageId()
        ));
    }

    // 2. Member Accepted Invitation -> Notify Fanpage Admin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvitationAccepted(FanpageInvitationAcceptedEvent event) {
        notificationCommandService.createNotification(CreateNotificationDto.forFanpage(
                event.fanpageId(),
                NotificationTypeEnum.FANPAGE_INVITATION_ACCEPTED,
                event.acceptedUserName() + " accepted the invitation to join fanpage",
                event.acceptedUserAvatarUrl(),
                event.fanpageId() + ""
        ));
    }

    // 2. Member Declined Invitation -> Notify Fanpage Admin
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInvitationRejected(FanpageInvitationRejectedEvent event) {
        notificationCommandService.createNotification(CreateNotificationDto.forFanpage(
                event.fanpageId(),
                NotificationTypeEnum.FANPAGE_INVITATION_REJECTED,
                event.rejectedUserName() + " declined the invitation to join fanpage",
                event.rejectedUserAvatarUrl(),
                event.fanpageId() + ""
        ));
    }

    // 2. Fanpage Admin Appointment
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onAdminAppointed(FanpageMemberAppointedEvent event) {
        notificationCommandService.createNotification(new CreateNotificationDto(
                event.appointedUserId(),
                NotificationTypeEnum.FANPAGE_ADMIN_APPOINTED,
                "You have been appointed as Admin of \"" + event.fanpageName() + "\" by " + event.appointerUserName() + ".",
                event.fanpageAvatarUrl(),
                "" + event.fanpageId()
        ));
    }

    // 3. Checkout Completed & Awarded
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCheckoutAwarded(EventCheckoutAwardedEvent event) {
        log.info("Notification listener running on thread: {}", Thread.currentThread().getName());
        List<CreateNotificationDto> notifications = event.userIds().stream()
                .map(userId -> new CreateNotificationDto(
                        userId,
                        NotificationTypeEnum.EVENT_CHECKOUT_AWARDED,
                        "You have been awarded for your checkout in the event \"" + event.eventName() + "\".",
                        event.eventAvatarUrl(),
                        "" + event.eventId()
                )).collect(Collectors.toList());
        notificationCommandService.createNotifications(notifications);
    }

    // 4. Event created for following fanpage
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventCreated(EventCreatedEvent event) {
        log.info("Notification listener running on thread: {}", Thread.currentThread().getName());
        List<CreateNotificationDto> notifications = event.followerUserIds().stream()
                .map(userId -> new CreateNotificationDto(
                        userId,
                        NotificationTypeEnum.EVENT_CREATED,
                        "A new event has been created for the fanpage you are following: \"" + event.eventName() + "\".",
                        event.eventAvatarUrl(),
                        "" + event.eventId()
                )).collect(Collectors.toList());
        notificationCommandService.createNotifications(notifications);
    }
}
