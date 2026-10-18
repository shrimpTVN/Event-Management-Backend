package com.ddd.api.mapper;

import com.ddd.api.dto.semesterpoint.res.PointTransactionResponseDto;
import com.ddd.application.dto.semesterpoint.PointTransactionInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PointTransactionApiMapper {
    PointTransactionResponseDto toResponseDto(PointTransactionInfoDto pointTransactionInfoDto);
}
