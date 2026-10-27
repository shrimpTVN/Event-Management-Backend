package com.ddd.application.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaCategory {
    AVATAR("avatars"),
    FANPAGE("fanpages"),
    EVENT("events"),
    EVIDENCE("evidence"),
    CERTIFICATE("certificates"),
    GENERAL("general");

    private final String folder;
}
