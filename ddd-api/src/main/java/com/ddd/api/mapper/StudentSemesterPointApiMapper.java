package com.ddd.api.mapper;

import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointFlattenedItemResponseDto;
import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointGroupedTreeResponseDto;
import com.ddd.api.dto.semesterpoint.res.StudentSemesterPointResponseDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointFlattenedItemDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointGroupedTreeDto;
import com.ddd.application.dto.semesterpoint.StudentSemesterPointInfoDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StudentSemesterPointApiMapper {
    StudentSemesterPointResponseDto toResponseDto(StudentSemesterPointInfoDto dto);

    StudentSemesterPointGroupedTreeResponseDto toGroupedTreeResponseDto(StudentSemesterPointGroupedTreeDto dto);

    List<StudentSemesterPointGroupedTreeResponseDto> toGroupedTreeResponseDtoList(List<StudentSemesterPointGroupedTreeDto> dtos);

    StudentSemesterPointFlattenedItemResponseDto toFlattenedItemResponseDto(StudentSemesterPointFlattenedItemDto dto);
}

