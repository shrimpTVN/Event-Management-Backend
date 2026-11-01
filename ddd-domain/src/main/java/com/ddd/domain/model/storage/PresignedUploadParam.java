package com.ddd.domain.model.storage;

public record PresignedUploadParam(
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
