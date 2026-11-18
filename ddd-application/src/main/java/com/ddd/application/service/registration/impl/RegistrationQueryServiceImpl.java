package com.ddd.application.service.registration.impl;

import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import com.ddd.application.dto.registration.StudentRegistrationInfoDto;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.registration.RegistrationQueryService;
import com.ddd.application.utils.ServiceUtil;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.EventJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegistrationQueryServiceImpl implements RegistrationQueryService {

    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationDtoMapper registrationDtoMapper;
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final EventJpaRepository eventJpaRepository;
    private final SecurityContextPort securityContextPort;
    private final UserRepository userRepository;


    @Override
    public Page<AttendeeInfoDto> getAttendeesOfEvent(Long fanpageId, Long eventId,
                                                     int page, int size, String sortBy, String sortDir) {
       verifyFanpageMember(fanpageId, eventId);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RegistrationJpaEntity> registrations = registrationJpaRepository.findAllByEventId(eventId, pageable);

        return registrations.map(registrationDtoMapper::toAttendeeInfoDto);
    }

    @Override
    public Page<StudentRegistrationInfoDto> getStudentRegistrationsOfEvent(Long fanpageId, Long eventId,
                                                                          int page, int size, String sortBy, String sortDir) {
        verifyFanpageMember(fanpageId, eventId);
        Pageable pageable = ServiceUtil.createPageable(page, size, sortBy, sortDir);
        Page<RegistrationJpaEntity> registrations = registrationJpaRepository.findAllByEventId(eventId, pageable);

        Set<Long> checkerIds = new HashSet<>();
        for (RegistrationJpaEntity reg : registrations) {
            if (reg.getCheckInBy() != null) checkerIds.add(reg.getCheckInBy());
            if (reg.getCheckOutBy() != null) checkerIds.add(reg.getCheckOutBy());
        }

        Map<Long, String> checkerNameMap = new HashMap<>();
        if (!checkerIds.isEmpty()) {
            List<User> checkers = userRepository.findAllByIds(checkerIds);
            for (User checker : checkers) {
                checkerNameMap.put(checker.getId(), formatFullName(checker.getFirstName(), checker.getLastName(), checker.getEmail()));
            }
        }

        return registrations.map(reg -> {
            String inName = reg.getCheckInBy() != null ? checkerNameMap.get(reg.getCheckInBy()) : null;
            String outName = reg.getCheckOutBy() != null ? checkerNameMap.get(reg.getCheckOutBy()) : null;
            return registrationDtoMapper.toStudentRegistrationInfoDto(reg, inName, outName);
        });
    }

    private String formatFullName(String firstName, String lastName, String email) {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? email : fullName;
    }

    @Override
    public Page<RegistrationInfoDto> getHistory(int page, int size) {
        Long userId = securityContextPort.getCurrentUserId();
        Pageable pageable = ServiceUtil.createPageable(page, size, "createdAt", "desc");

        Page<RegistrationJpaEntity> entityPage = registrationJpaRepository.findAllByUserId(userId, pageable);

        Set<Long> checkerIds = new HashSet<>();
        for (RegistrationJpaEntity reg : entityPage) {
            if (reg.getCheckInBy() != null) checkerIds.add(reg.getCheckInBy());
            if (reg.getCheckOutBy() != null) checkerIds.add(reg.getCheckOutBy());
        }

        Map<Long, String> checkerNameMap = new HashMap<>();
        if (!checkerIds.isEmpty()) {
            List<User> checkers = userRepository.findAllByIds(checkerIds);
            for (User checker : checkers) {
                checkerNameMap.put(checker.getId(), formatFullName(checker.getFirstName(), checker.getLastName(), checker.getEmail()));
            }
        }

        return entityPage.map(reg -> {
            String inName = reg.getCheckInBy() != null ? checkerNameMap.get(reg.getCheckInBy()) : null;
            String outName = reg.getCheckOutBy() != null ? checkerNameMap.get(reg.getCheckOutBy()) : null;
            return registrationDtoMapper.toInfoDto(reg, inName, outName);
        });
    }

    @Override
    public List<RegistrationInfoDto> getUpcoming() {
        Long userId = securityContextPort.getCurrentUserId();
        List<RegistrationJpaEntity> upcomingList = registrationJpaRepository.findUpcomingRegisteredEvents(userId);

        return upcomingList.stream()
                .map(registrationDtoMapper::toInfoDto)
                .toList();
    }

    private void verifyFanpageMember( Long fanpageId, Long eventId) {
       Long memberId = securityContextPort.getCurrentUserId();

        if (!fanpageMemberJpaRepository.existsByFanpageIdAndUserIdAndIsActive(fanpageId, memberId,  true)) {
            throw new BusinessException("User is not a member of the specified fanpage");
        }
       if (!eventJpaRepository.existsByIdAndFanpageId(eventId, fanpageId)) {
            throw new BusinessException("Event does not belong to the specified fanpage");
        }
    }
}
