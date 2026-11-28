package com.ddd.api.dto.fanpage.req;

import jakarta.validation.constraints.NotBlank;

public record RejectFanpageRequestDto(
        @NotBlank(message = "Rejection reason is required")
        String reason
) {
}
