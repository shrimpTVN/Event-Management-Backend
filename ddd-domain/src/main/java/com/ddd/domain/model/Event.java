package com.ddd.domain.model;

import com.ddd.domain.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    private boolean genderConstraint = false;
    private String bannerUrl;
    private String status;
    private String violationReason;
    private Long eventTypeId;
    private Long criteriaId;
    private Long fanpageId;
    private Long semesterId;
    private Long schoolId;
    private boolean allowSelfCheck = true;
    private List<EventPoint> eventPoints = new ArrayList<>();

    public void validateEventDates() {
        Instant now = Instant.now();
        if (dateOpen == null) {
            throw BusinessException.withField("dateOpen", "Registration open date is required");
        }
        if (dateClose == null) {
            throw BusinessException.withField("dateClose", "Registration close date is required");
        }
        if (dateHappen == null) {
            throw BusinessException.withField("dateHappen", "Event happen date is required");
        }
        if (dateOpen.isBefore(now)) {
            throw BusinessException.withField("dateOpen", "Registration open date cannot be in the past");
        }
        validateDateOrdering();
    }

    public void validateEventDatesForUpdate(Instant previousDateOpen) {
        if (dateOpen == null) {
            throw BusinessException.withField("dateOpen", "Registration open date is required");
        }
        if (dateClose == null) {
            throw BusinessException.withField("dateClose", "Registration close date is required");
        }
        if (dateHappen == null) {
            throw BusinessException.withField("dateHappen", "Event happen date is required");
        }
        // FE edits date/time at minute precision, so compare truncated to minutes to avoid false positives on unchanged dateOpen.
        boolean dateOpenChanged = previousDateOpen == null
                || !previousDateOpen.truncatedTo(ChronoUnit.MINUTES).equals(dateOpen.truncatedTo(ChronoUnit.MINUTES));
        if (dateOpenChanged && dateOpen.isBefore(Instant.now())) {
            throw BusinessException.withField("dateOpen", "Registration open date cannot be in the past");
        }
        validateDateOrdering();
    }

    private void validateDateOrdering() {
        if (dateOpen.isAfter(dateClose)) {
            throw BusinessException.withField("dateOpen", "Registration open date must be before or equal to registration close date");
        }
        if (dateClose.isAfter(dateHappen)) {
            throw BusinessException.withField("dateClose", "Registration close date must be before or equal to event happen date");
        }
    }

    public void validateCapacityAndQuantities() {
        if (maleQuantity < 0) {
            throw BusinessException.withField("maleQuantity", "Male and female quantities must be non-negative");
        }
        if (femaleQuantity < 0) {
            throw BusinessException.withField("femaleQuantity", "Male and female quantities must be non-negative");
        }
        if (maleQuantity + femaleQuantity > capacity) {
            String message = String.format(
                    "Total male and female quantities (%d) cannot exceed event capacity (%d)",
                    capacity
            );
            throw new BusinessException(message, java.util.Map.of(
                    "maleQuantity", message,
                    "femaleQuantity", message
            ));
        }
    }
}
