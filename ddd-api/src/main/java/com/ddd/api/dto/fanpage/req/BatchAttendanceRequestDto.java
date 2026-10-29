package com.ddd.api.dto.fanpage.req;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchAttendanceRequestDto(
        @NotEmpty(message = "studentUserIds must not be empty")
        List<Long> studentUserIds
) {
}
