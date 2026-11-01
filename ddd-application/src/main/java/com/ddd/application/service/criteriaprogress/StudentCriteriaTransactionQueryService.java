package com.ddd.application.service.criteriaprogress;

import com.ddd.application.dto.criteria.CriteriaTransactionInfoDto;

import java.util.List;

public interface StudentCriteriaTransactionQueryService {
    List<CriteriaTransactionInfoDto> getCriteriaTransactions(String email, Long criteriaId, Long academicYearId);
}
