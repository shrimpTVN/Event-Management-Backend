package com.ddd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FanpageMember {
    private Long fanpageId;
    private Long userId;
    private String role;

    private Instant createdAt;
    private Instant updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private boolean isActive = true;

    public FanpageMember(Long fanpageId, Long userId, String role) {
        this.fanpageId = fanpageId;
        this.userId = userId;
        this.role = role;
    }


}
