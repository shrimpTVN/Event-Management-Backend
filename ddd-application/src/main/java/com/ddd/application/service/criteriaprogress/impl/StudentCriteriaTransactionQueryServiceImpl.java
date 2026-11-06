package com.ddd.application.service.criteriaprogress.impl;

import com.ddd.application.dto.criteria.CriteriaTransactionInfoDto;
import com.ddd.application.mapper.CriteriaDtoMapper;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionQueryService;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.UserRepository;
import com.ddd.infrastructure.entity.StudentCriteriaTransactionJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.StudentCriteriaTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentCriteriaTransactionQueryServiceImpl implements StudentCriteriaTransactionQueryService {
    private final UserRepository userRepository;
    private final StudentCriteriaTransactionJpaRepository criteriaTransactionJpaRepository;
    private final CriteriaDtoMapper criteriaDtoMapper;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<CriteriaTransactionInfoDto> getCriteriaTransactions(Long criteriaId, Long academicYearId) {
        Long userId = securityContextPort.getCurrentUserId();
        List<StudentCriteriaTransactionJpaEntity> transactions = criteriaTransactionJpaRepository
                .findByUserIdAndCriteriaIdAndAcademicYearId(userId, criteriaId, academicYearId);
        return transactions.stream()
                .map(criteriaDtoMapper::toCriteriaTransactionInfoDto)
                .toList();
    }
}
