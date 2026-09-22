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
public class StudentSemesterPointId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "semester_id", nullable = false)
    private Long semesterId;
    @Column(name = "point_category_id", nullable = false)
    private Long pointCategoryId;

}
