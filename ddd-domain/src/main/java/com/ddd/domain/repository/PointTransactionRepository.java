package com.ddd.domain.repository;

import com.ddd.domain.model.PointTransaction;

import java.util.List;
import java.util.Optional;

public interface PointTransactionRepository {
    void saveAll(List<PointTransaction> pointTransactions);

    void save(PointTransaction pointTransaction);

    Optional<PointTransaction> findById(Long id);

    void delete(PointTransaction pointTransaction);
}
