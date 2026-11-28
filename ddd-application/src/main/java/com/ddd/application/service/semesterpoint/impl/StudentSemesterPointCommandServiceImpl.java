package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.model.PointTransaction;
import com.ddd.domain.model.StudentSemesterPoint;
import com.ddd.domain.repository.PointTransactionRepository;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentSemesterPointCommandServiceImpl implements StudentSemesterPointCommandService {
    private final StudentSemesterPointRepository points;
    private final PointTransactionRepository transactions;
    private final SemesterPointCategoryPolicy categoryPolicy;
    private final StudentSemesterPointCalculator calculator;

    @Override
    public void rebuildSemesterPoints(Long userId, Long semesterId) {
        rebuildSemesterPoints(List.of(userId), semesterId);
    }

    @Override
    public void rebuildSemesterPoints(List<Long> userIds, Long semesterId) {
        if (userIds == null || userIds.isEmpty()) return;
        var distinctUsers = userIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (distinctUsers.isEmpty()) return;
        var categories = categoryPolicy.effectiveCategories(semesterId);
        var ledger = transactions.findAllByUserIdsAndSemesterId(distinctUsers, semesterId).stream()
                .collect(Collectors.groupingBy(PointTransaction::getUserId));
        var stored = points.findAllByUserIdsAndSemesterId(distinctUsers, semesterId).stream()
                .collect(Collectors.groupingBy(StudentSemesterPoint::getUserId));
        List<StudentSemesterPoint> snapshots = new ArrayList<>();
        Instant now = Instant.now();
        for (Long userId : distinctUsers) {
            var existing = stored.getOrDefault(userId, List.of()).stream()
                    .collect(Collectors.toMap(StudentSemesterPoint::getPointCategoryId, Function.identity()));
            var result = calculator.calculate(userId, semesterId, categories, ledger.getOrDefault(userId, List.of()));
            for (var calculated : result.points()) {
                var snapshot = existing.remove(calculated.getPointCategoryId());
                if (snapshot == null) snapshot = calculated;
                snapshot.setTotalPoint(calculated.getTotalPoint());
                snapshot.setSystemPointRecord(calculated.getSystemPointRecord());
                snapshot.setManualPointRecord(calculated.getManualPointRecord());
                snapshot.setUpdatedAt(now);
                snapshots.add(snapshot);
            }
            for (var stale : existing.values()) {
                stale.setTotalPoint(0);
                stale.setSystemPointRecord(0);
                stale.setManualPointRecord(0);
                stale.setUpdatedAt(now);
                snapshots.add(stale);
            }
        }
        points.saveAll(snapshots);
    }
}
