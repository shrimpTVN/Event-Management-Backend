package com.ddd.api.dto.fanpage.req;

public record FanpageAdminUpdateRequestDto(
        String description,
        String avatarUrl,
        String helpContact,
        String helpPhone
) {
}
