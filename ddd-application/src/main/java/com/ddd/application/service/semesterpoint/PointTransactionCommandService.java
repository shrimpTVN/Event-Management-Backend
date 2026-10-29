package com.ddd.application.service.semesterpoint;

import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;

public interface PointTransactionCommandService {
    void createPointTransactionForStudent(String email, PointTransactionRequestInfoDto pointTransactionInfoDto);
}
