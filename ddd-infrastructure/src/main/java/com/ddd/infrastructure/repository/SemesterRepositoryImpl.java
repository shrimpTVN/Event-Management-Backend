package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
import com.ddd.infrastructure.mapper.SemesterMapper;
import com.ddd.infrastructure.repository.jpaRepository.SemesterJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Semester save(Semester semester) {
        SemesterJpaEntity semesterEntity = semesterMapper.toJpaEntity(semester);
        return semesterMapper.toDomain(semesterJpaRepository.save(semesterEntity));
    }

    @Override
    public Optional<Semester> findById(Long id) {
        return semesterJpaRepository.findById(id)
                .map(semesterMapper::toDomain);
    }

    @Override
    public Semester getCurrent() {
        SemesterJpaEntity semesterEntity = semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING)
                .orElseThrow(() -> new ResourceNotFoundException("Current semester not found"));
        return semesterMapper.toDomain(semesterEntity);
    }

    @Override
    public void delete(Long id) {
        SemesterJpaEntity semesterEntity = semesterJpaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + id));
        semesterEntity.setIsActive(false);
        semesterJpaRepository.save(semesterEntity);
    }

    @Override
    public boolean existsByNumberAndDateRange(Integer number, LocalDate startDate, LocalDate endDate) {
        return semesterJpaRepository.existsByNumberAndStartDateAndEndDate(number, startDate, endDate);
    }

    @Override
    public boolean existsOverlappingDateRange(LocalDate startDate, LocalDate endDate) {
        return semesterJpaRepository.existsByStartDateBeforeAndEndDateAfter(startDate, endDate);
    }
}
