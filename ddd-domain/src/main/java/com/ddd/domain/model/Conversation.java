package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Conversation {
    private Long id;
    private Long userId;
    private Long fanpageId;
    private Instant createdAt;
}
