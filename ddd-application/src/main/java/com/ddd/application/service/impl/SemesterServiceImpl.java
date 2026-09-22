package com.ddd.application.service.impl;

import com.ddd.application.dto.auth.SemesterDto;
import com.ddd.application.mapper.SemesterDtoMapper;
import com.ddd.application.service.SemesterService;
import com.ddd.domain.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;
    private final SemesterDtoMapper semesterDtoMapper;

    @Override
    public List<SemesterDto> findAll() {
        return semesterRepository.findAll().stream()
                .map(semesterDtoMapper::toDto)
                .toList();
    }
}
