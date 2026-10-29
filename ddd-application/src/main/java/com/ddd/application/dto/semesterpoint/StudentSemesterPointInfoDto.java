package com.ddd.application.dto.semesterpoint;

import java.time.Instant;

public record StudentSemesterPointInfoDto(
        Long id,
        Long pointCategoryId,
        Integer totalPoint,
        Instant finalizedAt
) {
}
