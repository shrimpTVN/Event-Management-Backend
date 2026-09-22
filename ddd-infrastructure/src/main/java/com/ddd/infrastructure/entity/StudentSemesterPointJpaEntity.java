package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "student_semester_points")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StudentSemesterPointJpaEntity {
    @EmbeddedId
    private StudentSemesterPointId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @MapsId("semesterId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterJpaEntity semester;

    @MapsId("pointCategoryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_category_id", nullable = false)
    private PointCategoryJpaEntity pointCategory;

    @Column(name = "total_point", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPoint = BigDecimal.ZERO;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
