package com.ddd.api.dto.school.res;

import com.ddd.domain.model.School;

public record SchoolResponseDto(Long id, String name, String description, boolean isActive) {}

