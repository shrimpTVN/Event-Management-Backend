package com.ddd.application.service.registration.impl;

import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.Registration;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.RegistrationRepository;
import com.ddd.domain.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.hibernate.sql.ast.tree.expression.Over;
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
    private final com.ddd.domain.repository.FanpageMemberRepository fanpageMemberRepository;

    @Override
    public void registerEvent(String email, Long eventId) {
        User user = userRepository.findByEmail(email);

        Event event = eventRepository.findById(eventId);
        if (!"PUBLISHED".equalsIgnoreCase(event.getStatus())) {
            throw new IllegalArgumentException("The event registration is not open yet or has already closed!");
        }

        if(registrationRepository.isRegistered(user.getId(), eventId)){
            throw new DuplicateResourceException("You had registered");
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

    @Override
    public void checkIn(String email, Long eventId){
        User user = userRepository.findByEmail(email);
        Registration registration = registrationRepository.findByUserAndEvent(user.getId(), eventId);
        Event event = eventRepository.findById(eventId);

        if(!LocalDate.now().isEqual(event.getDateHappen())){
            throw new IllegalArgumentException("This event has not started yet");
        }

        if(registration.getStatus() != RegistrationStatusEnum.REGISTERED){
            throw new IllegalArgumentException("You not registered");
        }

        registration.setCheckInAt(Instant.now());
        registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        registrationRepository.save(registration, user.getId(), eventId);
    }

    @Override
    public void checkOut(String email, Long eventId, String proofUrl){
        User user = userRepository.findByEmail(email);
        Registration registration = registrationRepository.findByUserAndEvent(user.getId(), eventId);
        Event event = eventRepository.findById(eventId);

        if(registration.getStatus() != RegistrationStatusEnum.CHECKED_IN){
            throw new IllegalArgumentException("You not check in");
        }
        registration.setCheckOutAt(Instant.now());
        registration.setEvidenceUrl(proofUrl);
        registration.setStatus(RegistrationStatusEnum.COMPLETED);
        registrationRepository.save(registration, user.getId(), eventId);
    }

    @Override
    public void adminCheckIn(String adminEmail, Long eventId, Long studentUserId) {
        User adminUser = userRepository.findByEmail(adminEmail);
        Event event = eventRepository.findById(eventId);

        FanpageMember fanpageMember = fanpageMemberRepository.findById(event.getFanpageId(), adminUser.getId())
                .orElseThrow(() -> new RuntimeException("You not have permission to manage this event"));
        
        if (!fanpageMember.isActive()) {
            throw new RuntimeException("Your account has been suspended");
        }

        Registration registration = registrationRepository.findByUserAndEvent(studentUserId, eventId);

        if(!LocalDate.now().isEqual(event.getDateHappen())){
            throw new RuntimeException("This event has not started yet");
        }

        if(registration.getStatus() != RegistrationStatusEnum.REGISTERED){
            throw new RuntimeException("This student has not registered for the event!");
        }

        registration.setCheckInAt(Instant.now());
        registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        registrationRepository.save(registration, studentUserId, eventId);
    }

    @Override
    public void adminCheckOut(String adminEmail, Long eventId, Long studentUserId) {
        User adminUser = userRepository.findByEmail(adminEmail);
        Event event = eventRepository.findById(eventId);

        FanpageMember fanpageMember = fanpageMemberRepository.findById(event.getFanpageId(), adminUser.getId())
                .orElseThrow(() -> new RuntimeException("You not have permission to manage this event"));
        
        if (!fanpageMember.isActive()) {
            throw new RuntimeException("Your account has been suspended");
        }
        
        Registration registration = registrationRepository.findByUserAndEvent(studentUserId, eventId);

        if(registration.getStatus() != RegistrationStatusEnum.CHECKED_IN){
            throw new RuntimeException("This student has not check in");
        }

        registration.setCheckOutAt(Instant.now());
        registration.setStatus(RegistrationStatusEnum.COMPLETED);
        registrationRepository.save(registration, studentUserId, eventId);
    }
}
