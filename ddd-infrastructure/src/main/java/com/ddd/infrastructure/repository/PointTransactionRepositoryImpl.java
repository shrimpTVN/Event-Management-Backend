package com.ddd.infrastructure.repository;

import com.ddd.domain.repository.PointTransactionRepository;
import com.ddd.infrastructure.repository.jpaRepository.PointTransactionJpaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PointTransactionRepositoryImpl implements PointTransactionRepository {
    private PointTransactionJpaRepository pointTransactionJpaRepository;

}
