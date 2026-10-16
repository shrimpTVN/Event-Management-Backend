package com.ddd.application.service.registration;

import java.util.List;

public interface RegistrationCommandService {
    void registerEvent(String email, Long eventId);

    void cancelRegistration(String email, Long eventId, String reason);

    void checkIn(String email, Long eventId, String proofUrl);

    void checkOut(String email, Long eventId, String proofUrl);

    void fanpageMemberCheckIn(String adminEmail, Long eventId, Long studentUserId);

    void fanpageMemberCheckIn(String adminEmail, Long eventId, List<Long> studentUserIds);

    void fanpageMemberCheckOut(String adminEmail, Long eventId, Long studentUserId);

    void fanpageMemberCheckOut(String adminEmail, Long eventId, List<Long> studentUserIds);
}
