package com.ddd.domain.enums;

public enum NotificationTypeEnum {
    // Fanpage Lifecycle
    FANPAGE_APPROVED,
    FANPAGE_REJECTED,

    // Fanpage Membership & Roles
    FANPAGE_MEMBER_INVITED,
    FANPAGE_INVITATION_ACCEPTED,
    FANPAGE_INVITATION_REJECTED,
    FANPAGE_ADMIN_APPOINTED,

    // Events
    EVENT_CREATED,
    EVENT_CHECKOUT_AWARDED,

    // Generic / Fallback
    SYSTEM
}
