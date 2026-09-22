package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "event_points")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventPointJpaEntity {
    @EmbeddedId
    private EventPointId id;

    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventJpaEntity event;

    @MapsId("pointCategoryId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_category_id", nullable = false)
    private PointCategoryJpaEntity pointCategory;

    @Column(name = "point", nullable = false)
    private Integer point = 0;

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
