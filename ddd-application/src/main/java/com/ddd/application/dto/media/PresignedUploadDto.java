package com.ddd.application.dto.media;

public record PresignedUploadDto(
        String uploadUrl,
        String apiKey,
        long timestamp,
        String signature,
        String folder,
        String assetFolder,
        String publicId,
        String cloudName
) {
}
