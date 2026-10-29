package com.ddd.application.service.registration.impl;

import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressCommandService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.enums.PointTransactionStatusEnum;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import com.ddd.domain.enums.RegistrationStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.EventAttendanceException;
import com.ddd.domain.exception.EventRegistrationException;
import com.ddd.domain.model.*;
import com.ddd.domain.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
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
    private final StudentSemesterPointRepository studentSemesterPointRepository;
    private final StudentCriteriaProgressCommandService studentCriteriaProgressService;
    private final StudentCriteriaTransactionCommandService studentCriteriaTransactionCommandService;

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

        Instant now = Instant.now();
        validateAndCompleteRegistration(registration, user.getId(), user.getId(), proofUrl, now);
        registrationRepository.save(registration, user.getId(), eventId);

        awardPointsForStudents(event, List.of(user.getId()), now);
        studentCriteriaTransactionCommandService.createTransactionFromEvent(user.getId(), eventId);
    }

    private void validateFanpageMemberPermission(User adminUser, Event event) {
        if (!LocalDate.now().isEqual(event.getDateHappen())) {
            throw new EventAttendanceException("This event has not started yet");
        }
        FanpageMember fanpageMember = fanpageMemberRepository.findById(event.getFanpageId(), adminUser.getId())
                .orElseThrow(() -> new EventAttendanceException("You not have permission to manage this event"));
        if (!fanpageMember.isActive()) {
            throw new BusinessException("Your account has been suspended");
        }
    }

    @Override
    public void fanpageMemberCheckIn(String fanpageMemberEmail, Long eventId, Long studentUserId) {
        fanpageMemberCheckIn(fanpageMemberEmail, eventId, List.of(studentUserId));
    }

    @Override
    public void fanpageMemberCheckOut(String fanpageMemberEmail, Long eventId, Long studentUserId) {
        fanpageMemberCheckOut(fanpageMemberEmail, eventId, List.of(studentUserId));
    }

    @Override
    public void fanpageMemberCheckIn(String fanpageMemberEmail, Long eventId, List<Long> studentUserIds) {
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return;
        }

        User fanpageMember = userRepository.findByEmail(fanpageMemberEmail);
        Event event = eventRepository.findById(eventId);
        validateFanpageMemberPermission(fanpageMember, event);

        List<Registration> registrations = registrationRepository.findAllByUserIdsAndEvent(studentUserIds, eventId);
        Map<Long, Registration> registrationMap = registrations.stream()
                .collect(Collectors.toMap(Registration::getUserId, r -> r));

        Instant now = Instant.now();
        for (Long studentUserId : studentUserIds) {
            Registration registration = registrationMap.get(studentUserId);
            if (registration == null) {
                throw new EventAttendanceException("Student ID " + studentUserId + " has not registered for this event!");
            }
            if (registration.getStatus() == RegistrationStatusEnum.COMPLETED) {
                throw new EventAttendanceException("Student ID " + studentUserId + " has already completed check-out!");
            }
            if (registration.getStatus() != RegistrationStatusEnum.CHECKED_IN) {
                throw new EventAttendanceException("Student ID " + studentUserId + " cannot check out! Current status is: " +
                        registration.getStatus());
            }
            registration.setCheckInAt(now);
            registration.setCheckInBy(fanpageMember.getId());
            registration.setStatus(RegistrationStatusEnum.CHECKED_IN);
        }

        registrationRepository.saveAll(registrations, eventId);
    }

    @Override
    public void fanpageMemberCheckOut(String fanpageMemberEmail, Long eventId, List<Long> studentUserIds) {
        if (studentUserIds == null || studentUserIds.isEmpty()) {
            return;
        }

        User fanpageMember = userRepository.findByEmail(fanpageMemberEmail);
        Event event = eventRepository.findById(eventId);
        validateFanpageMemberPermission(fanpageMember, event);

        List<Registration> registrations = registrationRepository.findAllByUserIdsAndEvent(studentUserIds, eventId);
        Map<Long, Registration> registrationMap = registrations.stream()
                .collect(Collectors.toMap(Registration::getUserId, Function.identity()));

        Instant now = Instant.now();
        for (Long studentUserId : studentUserIds) {
            validateAndCompleteRegistration(registrationMap.get(studentUserId),
                    studentUserId, fanpageMember.getId(), null, now);
        }

        registrationRepository.saveAll(registrations, eventId);
        awardPointsForStudents(event, studentUserIds, now);
        studentCriteriaTransactionCommandService.createTransactionFromEvent(studentUserIds, eventId);
    }

    private void validateAndCompleteRegistration(Registration registration, Long studentUserId, Long checkedOutById,
                                                 String proofUrl, Instant now) {
        if (registration == null) {
            throw new EventAttendanceException("Student ID " + studentUserId + " has not registered for the event!");
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


        List<StudentSemesterPoint> existingPoints = takeExistingPoints(studentUserIds, event, eventPoints);
        Map<StudentPointKey, StudentSemesterPoint> pointMap = existingPoints.stream()
                .collect(Collectors.toMap(
                        sp -> new StudentPointKey(sp.getUserId(), sp.getPointCategoryId()),
                        Function.identity()
                ));

        List<PointTransaction> pointTransactions = new ArrayList<>();


        for (Long studentUserId : studentUserIds) {
            for (EventPoint eventPoint : eventPoints) {

                pointTransactions.add(createPointTransaction(event, studentUserId, eventPoint));

                // Update or create StudentSemesterPoint
                StudentPointKey key = new StudentPointKey(studentUserId, eventPoint.getPointCategoryId());
                StudentSemesterPoint ssp = pointMap.get(key);
                if (ssp == null) {
                    ssp = new StudentSemesterPoint(studentUserId, event.getSemesterId(),
                            eventPoint.getPointCategoryId(), eventPoint.getPoint());
                    pointMap.put(key, ssp);
                } else {
                    ssp.setTotalPoint(ssp.getTotalPoint() + eventPoint.getPoint());
                    ssp.setUpdatedAt(now);
                }
            }
        }

        pointTransactionRepository.saveAll(pointTransactions);
        studentSemesterPointRepository.saveAll(new ArrayList<>(pointMap.values()));
    }

    private List<StudentSemesterPoint> takeExistingPoints(List<Long> studentUserIds, Event event,
                                                          List<EventPoint> eventPoints) {
        List<Long> pointCategoryIds = eventPoints.stream()
                .map(EventPoint::getPointCategoryId)
                .distinct()
                .toList();
        return studentSemesterPointRepository.findAllIn(studentUserIds, event.getSemesterId(), pointCategoryIds);
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

    record StudentPointKey(Long userId, Long pointCategoryId) {
    }
}
