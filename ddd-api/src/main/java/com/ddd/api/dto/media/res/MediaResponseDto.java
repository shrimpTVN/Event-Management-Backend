package com.ddd.api.dto.media.res;

public record MediaResponseDto(
        String publicId,
        String url,
        String secureUrl,
        String format,
        String resourceType,
        long bytes,
        int width,
        int height,
        String createdAt
) {
}
