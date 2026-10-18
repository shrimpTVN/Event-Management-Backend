package com.ddd.application.service.semesterpoint;

import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;

import java.util.List;

public interface PointTransactionQueryService {
    List<PointTransactionInfoDto> getPointTransactionsForStudent(String email, Long semesterId, Long pointCategoryId);
}
