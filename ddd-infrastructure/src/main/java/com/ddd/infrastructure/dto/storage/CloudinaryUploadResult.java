package com.ddd.infrastructure.dto.storage;

public record CloudinaryUploadResult(
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
