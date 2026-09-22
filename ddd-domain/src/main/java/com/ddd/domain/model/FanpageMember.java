package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class FanpageMember {
    private Long fanpageId;
    private Long userId;
    private String role;

    private Instant createAt;
    private Long createBy;
    private Instant updateAt;
    private Long updateBy;
}
