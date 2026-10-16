package com.ddd.domain.repository;

import com.ddd.domain.model.Registration;

import java.util.Collection;
import java.util.List;

public interface RegistrationRepository {
    boolean isRegistered(Long userId, Long eventId);
    void save(Registration registration, Long userId, Long eventId);
    void saveAll(List<Registration> registrations, Long eventId);

    Registration findByUserAndEvent(Long userId, Long eventId);
    List<Registration> findAllByUserIdsAndEvent(Collection<Long> userIds, Long eventId);
}
