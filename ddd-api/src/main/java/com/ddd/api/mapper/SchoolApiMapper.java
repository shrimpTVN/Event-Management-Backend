package com.ddd.api.mapper;


import com.ddd.api.dto.school.req.*;
import com.ddd.api.dto.school.res.*;
import com.ddd.application.dto.school.SchoolDto;
import com.ddd.application.dto.school.SchoolDetailDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SchoolApiMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    SchoolDto toSchoolDto(CreateSchoolRequestDto req);

    @Mapping(target = "id", ignore = true)
    SchoolDto toSchoolDto(UpdateSchoolRequestDto req);

    SchoolResponseDto toSchoolResponseDto(SchoolDto dto);
    SchoolDetailResponseDto toSchoolDetailResponseDto(SchoolDetailDto dto);
}
