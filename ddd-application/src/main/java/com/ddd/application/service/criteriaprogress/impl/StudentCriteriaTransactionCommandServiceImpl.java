package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.service.criteriaprogress.StudentCriteriaProgressCommandService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.domain.enums.CriteriaCompletionSourceEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.Semester;
import com.ddd.domain.model.StudentCriteriaTransaction;
import com.ddd.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StudentCriteriaTransactionCommandServiceImpl implements StudentCriteriaTransactionCommandService {
    private final StudentCriteriaTransactionRepository criteriaTransactionRepository;
    private final UserRepository userRepository;
    private final CriteriaRepository criteriaRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EventRepository eventRepository;
    private final SemesterRepository semesterRepository;
    private final StudentCriteriaProgressCommandService criteriaProgressCommandService;

    @Override
    public void createTransaction(String email, Long criteriaId, Long academicYearId, String note) {
        var user = userRepository.findByEmail(email);
        var criteria = criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new BusinessException("Criteria not found with id: " + criteriaId));
        var academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new BusinessException("Academic year not found with id: " + academicYearId));

        validateManualCriteriaEligible(criteria);

        var transaction = new StudentCriteriaTransaction(
                user.getId(),
                criteria.getId(),
                academicYear.getId(),
                null,
                CriteriaCompletionSourceEnum.MANUAL.name(),
                note
        );
        criteriaTransactionRepository.save(transaction);
        log.info("Transaction created with id: {}", transaction.getId());

        criteriaProgressCommandService.markCompleted(user.getId(), criteria.getId(), academicYear.getId());

    }

    private void validateManualCriteriaEligible(Criteria criteria) {
        if (!criteria.isActive()) {
            throw new BusinessException("Cannot set transaction for inactive criteria");
        }

        boolean isLeaf = !criteriaRepository.hasChildren(criteria.getId());
        if (!isLeaf) {
            throw new BusinessException("Only leaf criteria can be changed manually");
        }
    }

    public void createTransactionFromEvent(Long userId, Long eventId) {
        createTransactionFromEvent(List.of(userId), eventId);
    }

    public void createTransactionFromEvent(List<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Event event = eventRepository.findById(eventId);
        if (event == null || event.getCriteriaId() == null || event.getSemesterId() == null) {
            throw new ResourceNotFoundException("Event not found or missing criteria/semester for event");
        }

        Criteria criteria = criteriaRepository.findById(event.getCriteriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Criteria not found for event"));
        boolean isLeaf = !criteriaRepository.hasChildren(criteria.getId());

        if (isLeaf) {
            throw new BusinessException("Only leaf criteria can be completed from event");
        }

        Semester semester = semesterRepository.findById(event.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found for event"));

        bulkCreateTransactions(userIds, criteria.getId(), semester.getAcademicYearId(), eventId);
    }

    private void bulkCreateTransactions(List<Long> userIds, Long criteriaId, Long academicYearId, Long eventId) {
        List<StudentCriteriaTransaction> toSave = userIds.stream()
                .distinct()
                .map(userId -> new StudentCriteriaTransaction(
                        userId,
                        criteriaId,
                        academicYearId,
                        eventId,
                        CriteriaCompletionSourceEnum.EVENT_CHECKOUT.name(),
                        "Event completion"))
                .collect(Collectors.toList());

        log.info("Bulk creating transactions for {} users", userIds.size());
        criteriaTransactionRepository.saveAll(toSave);
    }
}
