package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class Event extends BaseModel {
    private String name;
    private String description;
    private Instant dateOpen;
    private Instant dateClose;
    private Instant dateHappen;
    private Integer capacity;
    private Integer maleQuantity;
    private Integer femaleQuantity;
    private String bannerUrl;
    private String aiScreeningResult;
    private BigDecimal aiScreeningScore;
    private String status;
    private Long eventTypeId;
    private Long criteriaId;
    private Long fanpageId;
}
