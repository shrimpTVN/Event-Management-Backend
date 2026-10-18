package com.ddd.api.mapper;

import com.ddd.api.dto.semesterpoint.req.PointTransactionRequestDto;
import com.ddd.api.dto.semesterpoint.res.PointTransactionResponseDto;
import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import com.ddd.application.dto.semesterpoint.PointTransactionRequestInfoDto;
import jakarta.validation.Valid;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointTransactionApiMapper {
    PointTransactionResponseDto toResponseDto(PointTransactionInfoDto pointTransactionInfoDto);

    PointTransactionRequestInfoDto toRequestInfoDto(@Valid PointTransactionRequestDto requestDto);
}
