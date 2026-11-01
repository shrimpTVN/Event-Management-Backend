package com.ddd.api.dto.media.res;

public record PresignedResponseDto(
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
