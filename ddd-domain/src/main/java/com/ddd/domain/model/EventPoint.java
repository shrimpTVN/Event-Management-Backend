package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class EventPoint extends BaseModel {
    private Long eventId;
    private Long pointCategoryId;
    private Integer point;
}
