package com.ddd.api.dto.school.res;
import com.ddd.api.dto.major.res.MajorResponseDto;
import java.util.List;

public record SchoolDetailResponseDto(Long id, String name, String description, boolean isActive, List<MajorResponseDto> majors) {}
