package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.mapper.PointTransactionDtoMapper;
import com.ddd.application.service.semesterpoint.PointTransactionQueryService;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.infrastructure.repository.jpaRepository.PointTransactionJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointTransactionQueryServiceImpl implements PointTransactionQueryService {
    private final PointTransactionJpaRepository pointTransactionJpaRepository;
    private final PointTransactionDtoMapper pointTransactionDtoMapper;
    private final UserJpaRepository userJpaRepository;
    private final SecurityContextPort securityContextPort;

    @Override
    public List<PointTransactionInfoDto> getPointTransactionsForStudent(Long semesterId,
                                                                        Long pointCategoryId) {
        Long userId = securityContextPort.getCurrentUserId();
        return pointTransactionJpaRepository
                .findByUserIdAndSemesterIdAndPointCategoryId(userId, semesterId, pointCategoryId)
                .stream()
                .map(pointTransactionDtoMapper::toInfoDto)
                .toList();
    }
}
