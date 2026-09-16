package com.ddd.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

//load all jwt properties from application.yml file
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        String cookieName,
        String header
) {
    public JwtProperties {
        // Validation khi khởi tạo
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters");
        }
    }
}
