package com.ddd.domain.repository;

import com.ddd.domain.model.PointTransaction;

import java.util.List;

public interface PointTransactionRepository {
    void saveAll(List<PointTransaction> pointTransactions);
}
