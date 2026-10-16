package com.ddd.application.service.registration.impl;

import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.application.mapper.RegistrationDtoMapper;
import com.ddd.application.service.registration.RegistrationQueryService;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.RegistrationJpaEntity;
import com.ddd.infrastructure.mapper.RegistrationMapper;
import com.ddd.infrastructure.repository.jpaRepository.RegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RegistrationQueryServiceImpl implements RegistrationQueryService {

    private final RegistrationJpaRepository registrationJpaRepository;
    private final UserRepository userRepository;
    private final RegistrationDtoMapper registrationDtoMapper;

    @Override
    public Page<RegistrationDto> getHistory(String email, int page, int size) {
        User user = userRepository.findByEmail(email);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<RegistrationJpaEntity> entityPage = registrationJpaRepository.findAllByUserId(user.getId(), pageable);

        return entityPage.map(registrationDtoMapper::toDto);

    }

    @Override
    public List<RegistrationDto> getUpcoming(String email) {
        User user = userRepository.findByEmail(email);

        List<RegistrationJpaEntity> upcomingList = registrationJpaRepository.findUpcomingRegisteredEvents(user.getId());

        return upcomingList.stream()
                .map(registrationDtoMapper::toDto)
                .toList();
    }
}
