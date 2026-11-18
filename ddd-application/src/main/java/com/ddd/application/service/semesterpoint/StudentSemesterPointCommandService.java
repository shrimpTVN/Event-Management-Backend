package com.ddd.application.service.semesterpoint;

import com.ddd.domain.enums.PointTransactionTypeEnum;

import java.util.List;
import java.util.Map;

public interface StudentSemesterPointCommandService {
    void recordPointAndPropagate(Long userId, Long semesterId, Long leafCategoryId, Integer points, PointTransactionTypeEnum type);

    void bulkRecordPointsAndPropagate(List<Long> userIds, Long semesterId, Map<Long, Integer> categoryPointsMap, PointTransactionTypeEnum type);
}
