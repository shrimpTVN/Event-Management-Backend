package com.ddd.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret,
        String folderPrefix,
        String uploadPreset
) {
    public CloudinaryProperties {
        if (folderPrefix == null || folderPrefix.isBlank()) {
            folderPrefix = "ctu-event";
        }
        if (uploadPreset == null || uploadPreset.isBlank()) {
            uploadPreset = "upload-CTUEvent";
        }
    }
}
