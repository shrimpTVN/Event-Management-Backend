package com.ddd.application.service.registration.impl;

import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.PointTransactionStatusEnum;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.model.*;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistrationCommandServiceImpl implements RegistrationCommandService {
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final EventPointRepository eventPointRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final StudentSemesterPointCommandService studentSemesterPointCommandService;
    private final StudentCriteriaTransactionCommandService studentCriteriaTransactionCommandService;
    private final SecurityContextPort securityContextPort;

    private Registration isRegistered(User user,  Long eventId) {
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
    public void registerEvent(Long eventId) {
        Event event = eventRepository.findById(eventId);
        Instant now = Instant.now();
        if (!EventStatusEnum.PUBLISHED.name().equalsIgnoreCase(event.getStatus())
                || (event.getDateClose() != null && event.getDateClose().isBefore(now))
                || (event.getDateOpen() != null && event.getDateOpen().isAfter(now))) {
            throw new EventRegistrationException("The event registration is not open yet or has already closed!");
        }

        String email = securityContextPort.getCurrentUserEmail();
        User user = userRepository.findByEmail(email);
        Registration reg = isRegistered(user, eventId);
        reg.setRegDate(Instant.now());
        reg.setStatus(RegistrationStatusEnum.REGISTERED);
        registrationRepository.save(reg, user.getId(), eventId);
    }

    @Override
    public void cancelRegistration(Long eventId, String reason) {
        Long userId = securityContextPort.getCurrentUserId();
        Event event = eventRepository.findById(eventId);
        Registration reg = registrationRepository.findByUserAndEvent(userId, eventId);

        if (event.getDateClose() != null &&
                Instant.now().isAfter(event.getDateClose())) {
            throw new EventRegistrationException("You can not CANCEL registration after event close");
        }

        if (reg.getStatus() != RegistrationStatusEnum.REGISTERED) {
            throw new EventRegistrationException("You can only cancel a registration that is currently registered.");
        }

        reg.setStatus(RegistrationStatusEnum.CANCELLED);
        reg.setReason(reason);

        registrationRepository.save(reg, userId, eventId);
    }

    private void validateEventAttendanceReadiness(Event event) {
        if (!event.isActive() || !EventStatusEnum.PUBLISHED.name().equalsIgnoreCase(event.getStatus())) {
            throw new EventAttendanceException("Cannot attend an event that is not active or published");
        }
        if (event.getDateHappen() == null) {
            throw new BusinessException("Event date has not been set");
        }
        if (Instant.now().isBefore(event.getDateHappen())) {
            throw new EventAttendanceException("This event has not started yet");
        }
    }

    @Override
    public void checkIn(Long eventId, String proofUrl) {
        Long userId = securityContextPort.getCurrentUserId();
        Event event = eventRepository.findById(eventId);
        validateEventAttendanceReadiness(event);

        Registration registration = registrationRepository.findByUserAndEvent(userId, eventId);

        if (registration.getStatus() == RegistrationStatusEnum.CHECKED_IN) {
            throw new EventAttendanceException("You are already checked in");
        }
        if (registration.getStatus() == RegistrationStatusEnum.COMPLETED) {
            throw new EventAttendanceException("You have already completed attendance for this event");
        }
        if (registration.getStatus() == RegistrationStatusEnum.CANCELLED) {
            throw new EventAttendanceException("Your registration for this event was cancelled");
        }
        if (registration.getStatus() != RegistrationStatusEnum.REGISTERED) {
            throw new EventAttendanceException("Invalid registration status for check-in: " + registration.getStatus());
        }

        registration.setCheckInAt(Instant.now());
        registration.setCheckInBy(userId);
        registration.setCheckInEvidenceUrl(proofUrl);
        registration.setStatus(RegistrationStatusEnum.CHECKED_IN);

        registrationRepository.save(registration, userId, eventId);
    }

    @Override
    public void checkOut(Long eventId, String proofUrl) {
        Long userId = securityContextPort.getCurrentUserId();
        Event event = eventRepository.findById(eventId);
        validateEventAttendanceReadiness(event);

        Registration registration = registrationRepository.findByUserAndEvent(userId, eventId);

        Instant now = Instant.now();
        validateAndCompleteRegistration(registration, userId, userId, proofUrl, now);
        registrationRepository.save(registration, userId, eventId);

        awardPointsForStudents(event, List.of(userId), now);
        if (event.getCriteriaId() != null) {
            studentCriteriaTransactionCommandService.createTransactionFromEvent(userId, eventId);
        }
    }

    private void validateFanpageMemberPermission(Long memberId, Event event) {
        if (!event.isActive() || !EventStatusEnum.PUBLISHED.name().equalsIgnoreCase(event.getStatus())) {
            throw new EventAttendanceException("Cannot manage attendance for an event that is not active or published");
        }
        if (event.getDateHappen() != null && Instant.now().isBefore(event.getDateHappen())) {
            throw new EventAttendanceException("This event has not started yet");
        }

        if (!fanpageMemberRepository.isActiveMember(event.getFanpageId(), memberId)) {
            throw new EventAttendanceException("You not have permission to manage this event");
        }
    }

    @Override
    public void fanpageMemberCheckIn(Long eventId, List<Long> studentUserIds) {
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return;
        }

        List<Long> distinctStudentUserIds = studentUserIds.stream().distinct().toList();
        Long fanpageMemberId = securityContextPort.getCurrentUserId();
        Event event = eventRepository.findById(eventId);
        validateFanpageMemberPermission(fanpageMemberId, event);

        List<Registration> registrations = registrationRepository.findAllByUserIdsAndEvent(distinctStudentUserIds, eventId);
        Map<Long, Registration> registrationMap = registrations.stream()
                .collect(Collectors.toMap(Registration::getUserId, r -> r));

        Instant now = Instant.now();
        for (Long studentUserId : distinctStudentUserIds) {
            Registration registration = registrationMap.get(studentUserId);
            if (registration == null) {
                throw new EventAttendanceException("Student ID " + studentUserId + " has not registered for this event!");
            }
            if (registration.getStatus() == RegistrationStatusEnum.CHECKED_IN) {
                throw new EventAttendanceException("Student ID " + studentUserId + " is already checked in!");
            }
            if (registration.getStatus() == RegistrationStatusEnum.COMPLETED) {
                throw new EventAttendanceException("Student ID " + studentUserId + " has already completed attendance!");
            }
            if (registration.getStatus() != RegistrationStatusEnum.REGISTERED) {
                throw new EventAttendanceException("Student ID " + studentUserId + " cannot check in! Current status is: " +
                        registration.getStatus());
            }
            registration.setCheckInAt(now);
            registration.setCheckInBy(fanpageMemberId);
            registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        }

        registrationRepository.saveAll(registrations, eventId);
    }

    @Override
    public void fanpageMemberCheckOut(Long eventId, List<Long> studentUserIds) {
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return;
        }

        List<Long> distinctStudentUserIds = studentUserIds.stream().distinct().toList();
        Long fanpageMemberId = securityContextPort.getCurrentUserId();
        Event event = eventRepository.findById(eventId);
        validateFanpageMemberPermission(fanpageMemberId, event);

        List<Registration> registrations = registrationRepository.findAllByUserIdsAndEvent(distinctStudentUserIds, eventId);
        Map<Long, Registration> registrationMap = registrations.stream()
                .collect(Collectors.toMap(Registration::getUserId, Function.identity()));

        Instant now = Instant.now();
        for (Long studentUserId : distinctStudentUserIds) {
            validateAndCompleteRegistration(registrationMap.get(studentUserId),
                    studentUserId, fanpageMemberId, null, now);
        }

        registrationRepository.saveAll(registrations, eventId);
        awardPointsForStudents(event, distinctStudentUserIds, now);

        if (event.getCriteriaId() != null) {
            studentCriteriaTransactionCommandService.createTransactionFromEvent(distinctStudentUserIds, eventId);
        }
    }

    private void validateAndCompleteRegistration(Registration registration, Long studentUserId, Long checkedOutById,
                                                 String proofUrl, Instant now) {
        if (registration == null) {
            throw new EventAttendanceException("Student ID " + studentUserId + " has not registered for the event!");
        }
        if (registration.getStatus() == RegistrationStatusEnum.COMPLETED) {
            throw new EventAttendanceException("Student ID " + studentUserId + " has already completed check-out!");
        }
        if (!registration.getStatus().equals(RegistrationStatusEnum.CHECKED_IN)) {
            throw new EventAttendanceException("Student ID " + studentUserId + " has not checked in!");
        }
        registration.setCheckOutAt(now);
        registration.setCheckOutBy(checkedOutById);
        if (proofUrl != null) {
            registration.setCheckOutEvidenceUrl(proofUrl);
        }
        registration.setStatus(RegistrationStatusEnum.COMPLETED);
    }

    private void awardPointsForStudents(Event event, List<Long> studentUserIds, Instant now) {
        List<EventPoint> eventPoints = eventPointRepository.findByEventId(event.getId());
        if (eventPoints.isEmpty() || studentUserIds == null || studentUserIds.isEmpty()) {
            return;
        }

        if (event.getSemesterId() == null) {
            throw new BusinessException("Event is not assigned to any semester");
        }

        List<PointTransaction> pointTransactions = new ArrayList<>();

        for (Long studentUserId : studentUserIds) {
            for (EventPoint eventPoint : eventPoints) {
                pointTransactions.add(createPointTransaction(event, studentUserId, eventPoint));
            }
        }

        pointTransactionRepository.saveAll(pointTransactions);

        // Batch propagate leaf points and all ancestors up to roots
        Map<Long, Integer> categoryPointsMap = eventPoints.stream()
                .collect(Collectors.toMap(
                        EventPoint::getPointCategoryId,
                        EventPoint::getPoint,
                        Integer::sum
                ));

        studentSemesterPointCommandService.bulkRecordPointsAndPropagate(
                studentUserIds,
                event.getSemesterId(),
                categoryPointsMap,
                PointTransactionTypeEnum.CHECKOUT_REWARD
        );
    }

    private PointTransaction createPointTransaction(Event event, Long studentUserId, EventPoint eventPoint) {
        PointTransaction pt = new PointTransaction();
        pt.setUserId(studentUserId);
        pt.setEventId(event.getId());
        pt.setPoints(eventPoint.getPoint());
        pt.setPointCategoryId(eventPoint.getPointCategoryId());
        pt.setSemesterId(event.getSemesterId());
        pt.setType(PointTransactionTypeEnum.CHECKOUT_REWARD.name());
        pt.setStatus(PointTransactionStatusEnum.CONFIRMED.name());
        pt.setDescription("Checkout reward for event: " + event.getName());
        return pt;
    }
}
