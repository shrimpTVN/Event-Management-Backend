package com.ddd.application.service.semesterpoint;

import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;

public interface PointTransactionCommandService {
    void createPointTransactionForStudent(PointTransactionRequestInfoDto pointTransactionInfoDto);

    void updatePointTransactionForStudent(Long id, Integer points, String description);

    void deletePointTransactionForStudent(Long pointTransactionId);
}
