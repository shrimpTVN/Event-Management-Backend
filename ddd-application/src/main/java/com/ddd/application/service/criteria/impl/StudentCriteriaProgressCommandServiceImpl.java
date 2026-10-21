package com.ddd.application.service.criteria.impl;

import com.ddd.application.service.academicyear.AcademicYearService;
import com.ddd.application.service.criteria.StudentCriteriaProgressCommandService;
import com.ddd.domain.enums.CriteriaCompletionSourceEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.AcademicYear;
import com.ddd.domain.model.Criteria;
import com.ddd.domain.model.Event;
import com.ddd.domain.model.Semester;
import com.ddd.domain.model.StudentCriteriaProgress;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.AcademicYearRepository;
import com.ddd.domain.repository.CriteriaRepository;
import com.ddd.domain.repository.EventRepository;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.domain.repository.StudentCriteriaProgressRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Command service handling write mutations and business rules for student criteria progress.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentCriteriaProgressCommandServiceImpl implements StudentCriteriaProgressCommandService {

    private final UserRepository userRepository;
    private final CriteriaRepository criteriaRepository;
    private final StudentCriteriaProgressRepository progressRepository;
    private final AcademicYearRepository academicYearRepository;
    private final EventRepository eventRepository;
    private final SemesterRepository semesterRepository;

    @Override
    public void setManualProgress(String email, Long criteriaId, Long academicYearId, String note) {
        User user = findUser(email);
        AcademicYear academicYear = findAcademicYear(academicYearId);
        Criteria criteria = findCriteria(criteriaId, "Criteria not found with id: " + criteriaId);

        //Only allow manual progress for leaf criteria and active criteria
        validateManualCriteriaEligible(criteria);

        StudentCriteriaProgress progress = new StudentCriteriaProgress(user.getId(), criteriaId, academicYear.getId(), note);
        progressRepository.save(progress);
    }

    @Override
    public void markCompletedFromEvent(Long userId, Long eventId) {
        markCompletedFromEvent(List.of(userId), eventId);
    }

    @Override
    public void markCompletedFromEvent(List<Long> userIds, Long eventId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        Event event = eventRepository.findById(eventId);
        if (event == null || event.getCriteriaId() == null || event.getSemesterId() == null) {
            throw new ResourceNotFoundException("Event not found or missing criteria/semester for event");
        }

        Criteria criteria = findCriteria(event.getCriteriaId(), "Criteria not found for event");
        if (!isLeaf(criteria.getId())) {
            throw new BusinessException("Only leaf criteria can be completed from event");
        }

        Semester semester = semesterRepository.findById(event.getSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found for event"));

        bulkCompleteProgress(userIds, criteria.getId(), semester.getAcademicYearId(), eventId);
    }

    private void bulkCompleteProgress(List<Long> userIds, Long criteriaId, Long academicYearId, Long eventId) {

        List<StudentCriteriaProgress> toSave = userIds.stream()
                .distinct()
                .map(userId -> new StudentCriteriaProgress(userId, criteriaId, academicYearId,"Event completion" , eventId))
                .collect(Collectors.toList());

        progressRepository.saveAll(toSave);
    }


    private boolean isLeaf(Long criteriaId) {
        return !criteriaRepository.hasChildren(criteriaId);
    }

    private void validateManualCriteriaEligible(Criteria criteria) {
        if (!criteria.isActive()) {
            throw new BusinessException("Cannot set progress for inactive criteria");
        }
        if (!isLeaf(criteria.getId())) {
            throw new BusinessException("Only leaf criteria can be changed manually");
        }
    }

    private User findUser(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return user;
    }

    private AcademicYear findAcademicYear(Long academicYearId) {
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private Criteria findCriteria(Long criteriaId, String errorMessage) {
        return criteriaRepository.findById(criteriaId)
                .orElseThrow(() -> new ResourceNotFoundException(errorMessage));
    }


}
