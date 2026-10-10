package com.ddd.application.service.registration;

public interface RegistrationCommandService {
    void registerEvent(String email, Long eventId);

    void cancelRegistration(String email, Long eventId, String reason);

    void checkIn(String email, Long eventId);

    void checkOut(String email, Long eventId, String proofUrl);

    void adminCheckIn(String adminEmail, Long eventId, Long studentUserId);

    void adminCheckOut(String adminEmail, Long eventId, Long studentUserId);
}
