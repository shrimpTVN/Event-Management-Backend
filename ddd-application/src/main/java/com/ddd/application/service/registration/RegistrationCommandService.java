package com.ddd.application.service.registration;

public interface RegistrationCommandService {
    void registerEvent(String email, Long eventId);
    void cancelRegistration(String email, Long eventId, String reason);
}
