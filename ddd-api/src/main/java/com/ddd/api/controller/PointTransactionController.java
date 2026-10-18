package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.semesterpoint.res.PointTransactionResponseDto;
import com.ddd.api.mapper.PointTransactionApiMapper;
import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.service.semesterpoint.PointTransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/point-transactions")
@RequiredArgsConstructor
public class PointTransactionController {
    private final PointTransactionQueryService pointTransactionQueryService;
    private final PointTransactionApiMapper pointTransactionApiMapper;

    @GetMapping("/me/semesters/{semesterId}/point-categories/{pointCategoryId}")
    public BaseResponse<List<PointTransactionResponseDto>> getPointTransactionsForStudent(Authentication authentication,
                                                                                          @PathVariable Long semesterId,
                                                                                          @PathVariable Long pointCategoryId) {
        String email = authentication.getName();
        List<PointTransactionInfoDto> pointTransactions = pointTransactionQueryService
                .getPointTransactionsForStudent(email, semesterId, pointCategoryId);
        return BaseResponse.of(pointTransactions.stream().map(pointTransactionApiMapper::toResponseDto)
                .collect(Collectors.toList()));
    }

}
