package com.ddd.application.dto.school;

import java.util.List;

public record SchoolDetailDto(
        Long id, String name, String description, boolean isActive, List<MajorDto> majors
) {}
