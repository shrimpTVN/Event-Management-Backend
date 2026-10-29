package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.criteria.req.CriteriaTransactionRequestDto;
import com.ddd.api.dto.criteria.res.CriteriaTransactionResponseDto;
import com.ddd.api.mapper.CriteriaApiMapper;
import com.ddd.application.dto.criteria.CriteriaTransactionInfoDto;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionCommandService;
import com.ddd.application.service.criteriaprogress.StudentCriteriaTransactionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/criteria-transactions")
@RestController
@RequiredArgsConstructor
public class CriteriaTransactionController {
    private final StudentCriteriaTransactionCommandService studentCriteriaTransactionCommandService;
    private final StudentCriteriaTransactionQueryService studentCriteriaTransactionQueryService;
    private final CriteriaApiMapper criteriaApiMapper;

    @PostMapping("/me")
    public BaseResponse<Void> createTransaction(Authentication authentication,
                                                @RequestBody @Valid CriteriaTransactionRequestDto requestDto) {
        studentCriteriaTransactionCommandService.createTransaction(
                authentication.getName(),
                requestDto.criteriaId(),
                requestDto.academicYearId(),
                requestDto.note()
        );
        return BaseResponse.ok();
    }

    @GetMapping("/me/criterias/{criteriaId}/academic-years/{academicYearId}")
    public BaseResponse<List<CriteriaTransactionResponseDto>> getCriteriaTransactions(
            Authentication authentication,
            @PathVariable Long criteriaId,
            @PathVariable Long academicYearId) {
        List<CriteriaTransactionInfoDto> transactions = studentCriteriaTransactionQueryService.getCriteriaTransactions(
                authentication.getName(),
                criteriaId,
                academicYearId
        );
        return BaseResponse.of(transactions.stream().map(criteriaApiMapper::toCriteriaTransactionResponseDto).toList());
    }
}
