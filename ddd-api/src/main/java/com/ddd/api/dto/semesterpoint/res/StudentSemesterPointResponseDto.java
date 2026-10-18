package com.ddd.api.dto.semesterpoint.res;

import java.time.Instant;

public record StudentSemesterPointResponseDto(
        Long id,
        Long pointCategoryId,
        Integer totalPoint,
        Instant finalizedAt
) {
}
