package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
public class Event extends BaseModel {
    private String name;
    private String description;
    private String location;
    private LocalDate dateOpen;
    private LocalDate dateClose;
    private LocalDate dateHappen;
    private Integer capacity;
    private Integer maleQuantity;
    private Integer femaleQuantity;
    private String bannerUrl;
    private String status;
    private Long eventTypeId;
    private Long criteriaId;
    private Long fanpageId;
    private Long semesterId;
}
