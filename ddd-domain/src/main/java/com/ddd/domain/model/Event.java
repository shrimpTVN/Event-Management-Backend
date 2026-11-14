package com.ddd.domain.model;

import com.ddd.domain.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Event extends BaseModel {
    private String name;
    private String description;
    private String location;
    private Instant dateOpen;
    private Instant dateClose;
    private Instant dateHappen;
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
        Instant now = Instant.now();
        if (dateOpen.isBefore(now)) {
            throw new BusinessException("Registration open date cannot be in the past");
        }
        if (dateOpen.isAfter(dateClose)) {
            throw new BusinessException("Registration open date must be before or equal to registration close date");
        }
        if (dateClose.isAfter(dateHappen)) {
            throw new BusinessException("Registration close date must be before or equal to event happen date");
        }
    }

    public void validateCapacityAndQuantities() {
        if (maleQuantity < 0 || femaleQuantity < 0) {
            throw new BusinessException("Male and female quantities must be non-negative");
        }
        if (maleQuantity + femaleQuantity > capacity) {
            throw new BusinessException(String.format(
                    "Total male and female quantities (%d) cannot exceed event capacity (%d)",
                    maleQuantity + femaleQuantity, capacity
            ));
        }
    }
}
