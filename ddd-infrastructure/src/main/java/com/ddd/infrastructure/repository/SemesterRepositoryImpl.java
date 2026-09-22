package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.SemesterStatusEnum;
import com.ddd.domain.model.Semester;
import com.ddd.domain.repository.SemesterRepository;
import com.ddd.infrastructure.entity.SemesterJpaEntity;
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

    @Override
    public Semester save(Semester semester) {
        //check whether semester is already exists with same number and time
        if (semesterJpaRepository.existsByNumberAndStartDateAndEndDate(semester.getNumber(), semester.getStartDate(), semester.getEndDate())) {
            throw new IllegalArgumentException("Semester already exists");
        }

        //check whether semester time is included in another semester time
        if (semesterJpaRepository.existsByStartDateBeforeAndEndDateAfter(semester.getStartDate(), semester.getEndDate())) {
            throw new IllegalArgumentException("Semester time is included in another semester time");
        }

        SemesterJpaEntity semesterEntity = semesterMapper.toJpaEntity(semester);
        return semesterMapper.toDomain(semesterJpaRepository.save(semesterEntity));
    }

    @Override
    public Semester findById(Long id) {
        SemesterJpaEntity semesterEntity = semesterJpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        return semesterMapper.toDomain(semesterEntity);
    }

    @Override
    public Semester getCurrent() {
        SemesterJpaEntity semesterEntity = semesterJpaRepository.findByStatus(SemesterStatusEnum.ONGOING.name())
                .orElseThrow(() -> new IllegalArgumentException("Current semester not found"));
        return semesterMapper.toDomain(semesterEntity);
    }
}
