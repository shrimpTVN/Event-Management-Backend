package com.model;

import lombok.*;

import java.time.Instant;

@Setter
@Getter
@RequiredArgsConstructor
public abstract class BaseModel {
    private Long id;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private boolean isActive = true;


}