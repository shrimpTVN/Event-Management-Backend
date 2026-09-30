package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private List<EventPoint> eventPoints = new ArrayList<>();

    public void validateEventDates() {
        LocalDate today = LocalDate.now();
        if (dateOpen.isBefore(today)) {
            throw new IllegalArgumentException("Registration open date cannot be in the past");
        }
        if (dateOpen.isAfter(dateClose)) {
            throw new IllegalArgumentException("Registration open date must be before or equal to registration close date");
        }
        if (dateClose.isAfter(dateHappen)) {
            throw new IllegalArgumentException("Registration close date must be before or equal to event happen date");
        }
    }

    public void validateCapacityAndQuantities() {
        if (maleQuantity < 0 || femaleQuantity < 0) {
            throw new IllegalArgumentException("Male and female quantities must be non-negative");
        }
        if (maleQuantity + femaleQuantity > capacity) {
            throw new IllegalArgumentException(String.format(
                    "Total male and female quantities (%d) cannot exceed event capacity (%d)",
                    maleQuantity + femaleQuantity, capacity
            ));
        }
    }
}
