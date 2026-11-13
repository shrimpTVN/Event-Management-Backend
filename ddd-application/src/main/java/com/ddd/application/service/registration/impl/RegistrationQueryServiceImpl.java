package com.ddd.application.service.registration.impl;

import com.ddd.application.dto.export.AttendeeExportRowDto;
import com.ddd.application.dto.export.EventAttendeeExportDto;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationArrangedInfoDto;
import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.registration.RegistrationQueryService;
import com.ddd.application.utils.CsvExportUtil;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.EventJpaEntity;
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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegistrationQueryServiceImpl implements RegistrationQueryService {
    private final RegistrationJpaRepository registrationJpaRepository;
    private final RegistrationDtoMapper registrationDtoMapper;
    private final FanpageMemberJpaRepository fanpageMemberJpaRepository;
    private final EventJpaRepository eventJpaRepository;
    private final SecurityContextPort securityContextPort;

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
    public Page<RegistrationInfoDto> getHistory(int page, int size) {
        Long userId = securityContextPort.getCurrentUserId();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RegistrationJpaEntity> entityPage = registrationJpaRepository.findAllByUserId(userId, pageable);

        return entityPage.map(registrationDtoMapper::toInfoDto);

    }

    @Override
    public List<RegistrationInfoDto> getUpcoming() {
        Long userId = securityContextPort.getCurrentUserId();
        List<RegistrationJpaEntity> upcomingList = registrationJpaRepository.findUpcomingRegisteredEvents(userId);

        return upcomingList.stream()
                .map(registrationDtoMapper::toInfoDto)
                .toList();
    }

    @Override
    public byte[] exportAttendeesToCsv(Long fanpageId, Long eventId) {
        verifyFanpageMember(fanpageId, eventId);

        List<AttendeeExportRowDto> attendeeRows = new ArrayList<>();
        List<RegistrationJpaEntity> registrations = registrationJpaRepository.findAllByEventId(eventId);
        for (int i = 0; i < registrations.size(); i++) {
            RegistrationJpaEntity registration = registrations.get(i);
            AttendeeExportRowDto rowDto = registrationDtoMapper.toAttendeeExportRowDto(registration, i + 1);
            attendeeRows.add(rowDto);
        }
        EventJpaEntity eventEntity = eventJpaRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException("Event not found with eventId: " + eventId));

        return CsvExportUtil.generateAttendeesCsv(new EventAttendeeExportDto(
                eventEntity.getId(),
                eventEntity.getName(),
                eventEntity.getLocation(),
                eventEntity.getDateHappen(),
                eventEntity.getDateOpen(),
                eventEntity.getDateClose(),
                eventEntity.getCapacity(),
                eventEntity.getStatus().name(),
                attendeeRows
        ));
    }

    @Override
    public List<RegistrationArrangedInfoDto> getArranged(Instant startTime, Instant endTime) {
        Long userId = securityContextPort.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("Authentication required to access arranged events");
        }

        if (startTime == null || endTime == null) {
            throw new BusinessException("Start time and end time must be provided");
        }

        if (startTime.isAfter(endTime)) {
            throw new BusinessException("Start time must be before end time");
        }

        if (Duration.between(startTime, endTime).toDays() > 40) {
            throw new BusinessException("The time range should not over one month.");
        }
        List<RegistrationJpaEntity> arrangedEvents = registrationJpaRepository.findArrangedEvents(userId, startTime, endTime);
        return arrangedEvents.stream()
                .map(registrationDtoMapper::toArrangedInfoDto)
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
