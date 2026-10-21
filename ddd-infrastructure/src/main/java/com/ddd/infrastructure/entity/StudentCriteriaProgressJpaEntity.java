package com.ddd.infrastructure.entity;

import com.ddd.domain.enums.CriteriaCompletionSourceEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "student_criteria_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_student_criteria_progress",
                columnNames = {"user_id", "criteria_id", "academic_year_id"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StudentCriteriaProgressJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "criteria_id", nullable = false)
    private Long criteriaId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "event_id")
    private Long sourceEventId;

    @Column(name = "is_completed", nullable = false)
    private Boolean completed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_source", nullable = false, length = 30)
    private CriteriaCompletionSourceEnum completionSource = CriteriaCompletionSourceEnum.MANUAL;



    @Column(name = "completed_at")
    private Instant completedAt;
}
