package com.ddd.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // CRITICAL: Required by JPA for composite keys
@Embeddable
public class EventPointId implements Serializable {

    @Column(name = "event_id")
    private Long event;

    @Column(name = "point_category_id")
    private Long pointCategory;

}
