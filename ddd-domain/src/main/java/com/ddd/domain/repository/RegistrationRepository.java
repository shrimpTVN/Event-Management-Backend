package com.ddd.domain.repository;

import com.ddd.domain.model.Registration;

public interface RegistrationRepository {
    boolean isRegistered(Long userId, Long eventId);
    void save(Registration registration, Long userId, Long eventId);


    Registration findByUserAndEvent(Long userId, Long eventId);
}
