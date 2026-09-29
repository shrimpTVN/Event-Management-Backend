package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_points")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventPointJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventJpaEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_category_id", nullable = false)
    private PointCategoryJpaEntity pointCategory;

    @Column(name = "point", nullable = false)
    private Integer point = 0;


}
