package com.ddd.application.service.registration.impl;

import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.Registration;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.RegistrationRepository;
import com.ddd.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrationCommandServiceImpl implements RegistrationCommandService {
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public void registerEvent(String email, Long eventId) {
        User user = userRepository.findByEmail(email);

        Event event = eventRepository.findById(eventId);
        if (!"PUBLISHED".equalsIgnoreCase(event.getStatus())) {
            throw new IllegalArgumentException("The event registration is not open yet or has already closed!");
        }

        if(registrationRepository.isRegistered(user.getId(), eventId)){
            throw new DuplicateResourceException("You had registrated");
        }

        Registration reg = new Registration();
        reg.setRegDate(Instant.now());
        reg.setStatus(RegistrationStatusEnum.REGISTERED);

        registrationRepository.save(reg, user.getId(), eventId);
    }

    @Override
    public void cancelRegistration(String email, Long eventId, String reason) {
        User user = userRepository.findByEmail(email);

        Registration reg = registrationRepository.findByUserAndEvent(user.getId(), eventId);

        Event event = eventRepository.findById(eventId);
        if(event.getDateHappen() != null &&
                LocalDate.now().isAfter(event.getDateHappen())){
            throw  new IllegalArgumentException("Can not CANCEl");
        }

        reg.setStatus(RegistrationStatusEnum.CANCELLED);
        reg.setReason(reason);

        registrationRepository.save(reg, user.getId(), eventId);
    }
}
