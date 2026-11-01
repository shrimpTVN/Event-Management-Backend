package com.ddd.application.dto.media;

public record MediaInfoDto(
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
