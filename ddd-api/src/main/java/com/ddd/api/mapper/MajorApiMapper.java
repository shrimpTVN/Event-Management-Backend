package com.ddd.api.mapper;
import com.ddd.api.dto.major.req.CreateMajorRequestDto;
import com.ddd.api.dto.major.req.UpdateMajorRequestDto;
import com.ddd.api.dto.major.res.MajorResponseDto;
import com.ddd.application.dto.school.MajorDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface MajorApiMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    MajorDto toMajorDto(CreateMajorRequestDto req);

    @Mapping(target = "id", ignore = true)
    MajorDto toMajorDto(UpdateMajorRequestDto req);

    MajorResponseDto toMajorResponseDto(MajorDto dto);
}
