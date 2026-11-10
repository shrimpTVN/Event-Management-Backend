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
        for (Long userId : event.userIds()) {
            notificationCommandService.createNotification(new CreateNotificationDto(
                    userId,
                    NotificationTypeEnum.EVENT_CHECKOUT_AWARDED,
                    "Congratulations! You completed attendance for \"" + event.eventName() + "\" and your criteria points have been awarded.",
                    event.eventAvatarUrl(),
                    "/events/" + event.eventId()
            ));
        }
    }

    // 4. Event created for following fanpage
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onEventCreated(EventCreatedEvent event) {
        for (Long userId : event.followerUserIds()) {
            notificationCommandService.createNotification(new CreateNotificationDto(
                    userId,
                    NotificationTypeEnum.EVENT_CREATED,
                    "A new event \"" + event.eventName() + "\" has been created by fanpage \"" + event.fanpageName() + "\".",
                    event.eventAvatarUrl(),
                    "" + event.eventId()
            ));
        }
    }
}
