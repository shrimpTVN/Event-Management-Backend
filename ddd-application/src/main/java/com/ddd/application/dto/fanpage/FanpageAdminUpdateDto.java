package com.ddd.application.dto.fanpage;

public record FanpageAdminUpdateDto(
        String description,
        String avatarUrl,
        String helpContact,
        String helpPhone
) {
}
