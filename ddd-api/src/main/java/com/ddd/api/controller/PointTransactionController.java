package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.semesterpoint.req.PointTransactionRequestDto;
import com.ddd.api.dto.semesterpoint.res.PointTransactionResponseDto;
import com.ddd.api.mapper.PointTransactionApiMapper;
import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import com.ddd.application.service.semesterpoint.PointTransactionCommandService;
import com.ddd.application.service.semesterpoint.PointTransactionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/point-transactions")
@RequiredArgsConstructor
public class PointTransactionController {
    private final PointTransactionQueryService pointTransactionQueryService;
    private final PointTransactionCommandService pointTransactionCommandService;
    private final PointTransactionApiMapper pointTransactionApiMapper;

    @GetMapping("/me/semesters/{semesterId}/point-categories/{pointCategoryId}")
    public BaseResponse<List<PointTransactionResponseDto>> getPointTransactionsForStudent(@PathVariable Long semesterId,
                                                                                          @PathVariable Long pointCategoryId) {
        List<PointTransactionInfoDto> pointTransactions = pointTransactionQueryService
                .getPointTransactionsForStudent(semesterId, pointCategoryId);
        return BaseResponse.of(pointTransactions.stream().map(pointTransactionApiMapper::toResponseDto)
                .collect(Collectors.toList()));
    }

    @PostMapping("/me")
    public BaseResponse<Void> createPointTransactionForStudent(@RequestBody @Valid PointTransactionRequestDto requestDto) {

        PointTransactionRequestInfoDto requestInfoDto = pointTransactionApiMapper.toRequestInfoDto(requestDto);
        pointTransactionCommandService
                .createPointTransactionForStudent(requestInfoDto);
        return BaseResponse.ok();
    }

    @DeleteMapping("/me/{pointTransactionId}")
    public BaseResponse<Void> deletePointTransactionForStudent(@PathVariable Long pointTransactionId){
        pointTransactionCommandService.deletePointTransactionForStudent(pointTransactionId);
        return BaseResponse.ok();
    }

}
