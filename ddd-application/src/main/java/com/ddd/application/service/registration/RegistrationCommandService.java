package com.ddd.application.service.registration;

import java.util.List;

public interface RegistrationCommandService {
    void registerEvent(String email, Long eventId);

    void cancelRegistration(String email, Long eventId, String reason);

    void checkIn(Long eventId, String proofUrl);

    void checkOut(Long eventId, String proofUrl);

    void fanpageMemberCheckIn(Long eventId, List<Long> studentUserIds);

    void fanpageMemberCheckOut(Long eventId, List<Long> studentUserIds);
}
