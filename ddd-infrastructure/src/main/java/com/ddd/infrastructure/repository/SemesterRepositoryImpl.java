package com.ddd.infrastructure.repository;

import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.infrastructure.mapper.SemesterMapper;
import com.ddd.infrastructure.repository.jpaRepository.SemesterJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SemesterRepositoryImpl implements SemesterRepository {
    private final SemesterJpaRepository semesterJpaRepository;
    private final SemesterMapper semesterMapper;

    @Override
    public List<Semester> findAll() {
        return semesterJpaRepository.findAllByIsActive(true).stream()
                .map(semesterMapper::toDomain).collect(Collectors.toList());
    }
}
