package com.ddd.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaCategoryEnum {
    AVATAR("avatars"),
    FANPAGE("fanpages"),
    EVENT("events"),
    EVIDENCE("evidence"),
    CERTIFICATE("certificates"),
    GENERAL("general");

    private final String folder;
}
