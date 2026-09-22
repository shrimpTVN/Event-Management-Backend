package com.ddd.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;


@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class RegistrationId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private Long user;
    @Column(name = "event_id", nullable = false)
    private Long event;


}
