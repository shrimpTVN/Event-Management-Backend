package com.ddd.application.dto.school;

public record MajorDto(Long id, String name, String code, Long schoolId, boolean isActive) {
}