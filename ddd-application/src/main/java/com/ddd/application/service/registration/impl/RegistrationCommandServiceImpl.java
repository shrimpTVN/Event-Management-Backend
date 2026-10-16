package com.ddd.application.service.registration.impl;

import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.Registration;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.FanpageMemberRepository;
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
    private final FanpageMemberRepository fanpageMemberRepository;

    private Registration isRegistered(User user, Long userId, Long eventId) {
        if (registrationRepository.isRegistered(user.getId(), eventId)) {
            Registration registration = registrationRepository.findByUserAndEvent(user.getId(), eventId);
            if (registration.getStatus() == RegistrationStatusEnum.CANCELLED) {
                return registration;
            }
            throw new DuplicateResourceException("You had registered");
        } else {
            return new Registration();
        }
    }

    @Override
    public void registerEvent(String email, Long eventId) {
        User user = userRepository.findByEmail(email);

        Event event = eventRepository.findById(eventId);
        if (!EventStatusEnum.PUBLISHED.name().equalsIgnoreCase(event.getStatus())
                || event.getDateClose().isBefore(LocalDate.now())
                || event.getDateOpen().isAfter(LocalDate.now())) {
            throw new EventRegistrationException("The event registration is not open yet or has already closed!");
        }

        Registration reg = isRegistered(user, user.getId(), eventId);
        reg.setRegDate(Instant.now());
        reg.setStatus(RegistrationStatusEnum.REGISTERED);
        registrationRepository.save(reg, user.getId(), eventId);
    }


    @Override
    public void cancelRegistration(String email, Long eventId, String reason) {
        User user = userRepository.findByEmail(email);
        Event event = eventRepository.findById(eventId);
        Registration reg = registrationRepository.findByUserAndEvent(user.getId(), eventId);

        if (event.getDateClose() != null &&
                LocalDate.now().isAfter(event.getDateClose())) {
            throw new EventRegistrationException("You can not CANCEL registration after event close");
        }

        if (reg.getStatus() != RegistrationStatusEnum.REGISTERED) {
            throw new EventRegistrationException("You can only cancel a registration that is currently registered.");
        }

        reg.setStatus(RegistrationStatusEnum.CANCELLED);
        reg.setReason(reason);

        registrationRepository.save(reg, user.getId(), eventId);
    }

    @Override
    public void checkIn(String email, Long eventId, String proofUrl) {
        User user = userRepository.findByEmail(email);
        Event event = eventRepository.findById(eventId);
        Registration registration = registrationRepository.findByUserAndEvent(user.getId(), eventId);


        if (!LocalDate.now().isEqual(event.getDateHappen())) {
            throw new EventAttendanceException("This event has not started yet");
        }

        if (registration.getStatus() != RegistrationStatusEnum.REGISTERED) {
            throw new EventAttendanceException("You have not registered");
        }

        registration.setCheckInAt(Instant.now());
        registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        registration.setCheckInBy(user.getId());
        registration.setCheckInEvidenceUrl(proofUrl);

        registrationRepository.save(registration, user.getId(), eventId);
    }

    @Override
    public void checkOut(String email, Long eventId, String proofUrl) {
        User user = userRepository.findByEmail(email);
        Event event = eventRepository.findById(eventId);
        Registration registration = registrationRepository.findByUserAndEvent(user.getId(), eventId);


        if (registration.getStatus() != RegistrationStatusEnum.CHECKED_IN) {
            throw new EventAttendanceException("You have not checked in");
        }
        registration.setCheckOutAt(Instant.now());
        registration.setCheckOutEvidenceUrl(proofUrl);
        registration.setCheckOutBy(user.getId());
        registration.setStatus(RegistrationStatusEnum.COMPLETED);
        registrationRepository.save(registration, user.getId(), eventId);
    }

    private Registration getRegistrationForFanpageMember(User adminUser, Long eventId, Long studentUserId) {
        Event event = eventRepository.findById(eventId);

        if (!LocalDate.now().isEqual(event.getDateHappen())) {
            throw new EventAttendanceException("This event has not started yet");
        }

        FanpageMember fanpageMember = fanpageMemberRepository.findById(event.getFanpageId(), adminUser.getId())
                .orElseThrow(() -> new EventAttendanceException("You not have permission to manage this event"));

        if (!fanpageMember.isActive()) {
            throw new BusinessException("Your account has been suspended");
        }

        return registrationRepository.findByUserAndEvent(studentUserId, eventId);
    }

    @Override
    public void adminCheckIn(String adminEmail, Long eventId, Long studentUserId) {
        User adminUser = userRepository.findByEmail(adminEmail);
        Registration registration = getRegistrationForFanpageMember(adminUser, eventId, studentUserId);

        if (registration.getStatus() != RegistrationStatusEnum.REGISTERED) {
            throw new EventAttendanceException("This student has not registered for the event!");
        }

        registration.setCheckInAt(Instant.now());
        registration.setCheckInBy(adminUser.getId());
        registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        registrationRepository.save(registration, studentUserId, eventId);
    }

    @Override
    public void adminCheckOut(String adminEmail, Long eventId, Long studentUserId) {
        User adminUser = userRepository.findByEmail(adminEmail);
        Registration registration = getRegistrationForFanpageMember(adminUser, eventId, studentUserId);

        if (registration.getStatus() != RegistrationStatusEnum.CHECKED_IN) {
            throw new EventAttendanceException("This student has not check in");
        }

        registration.setCheckOutAt(Instant.now());
        registration.setCheckOutBy(adminUser.getId());
        registration.setStatus(RegistrationStatusEnum.COMPLETED);
        registrationRepository.save(registration, studentUserId, eventId);
    }
}
