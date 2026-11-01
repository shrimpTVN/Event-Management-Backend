package com.ddd.api.dto.media.req;

public record PresignedRequestDto(
        com.ddd.domain.enums.MediaCategoryEnum category,
        String fileExtension
) {
}
