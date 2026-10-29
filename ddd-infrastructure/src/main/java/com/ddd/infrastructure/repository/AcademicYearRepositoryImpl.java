package com.ddd.infrastructure.repository;

import com.ddd.domain.model.AcademicYear;
import com.ddd.domain.repository.AcademicYearRepository;
import com.ddd.infrastructure.mapper.AcademicYearMapper;
import com.ddd.infrastructure.repository.jpaRepository.AcademicYearJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AcademicYearRepositoryImpl implements AcademicYearRepository {
    private final AcademicYearJpaRepository jpaRepository;
    private final AcademicYearMapper mapper;

    @Override
    public List<AcademicYear> findAllActive() {
        return mapper.toDomainList(jpaRepository.findAllByIsActiveTrueOrderByStartDateDesc());
    }

    @Override
    public Optional<AcademicYear> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<AcademicYear> findByCode(String code) {
        return jpaRepository.findByCode(code).map(mapper::toDomain);
    }

    @Override
    public Optional<AcademicYear> findCurrent(LocalDate date) {
        return jpaRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualAndIsActiveTrue(date, date)
                .map(mapper::toDomain);
    }

    @Override
    public AcademicYear save(AcademicYear academicYear) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(academicYear)));
    }
}
