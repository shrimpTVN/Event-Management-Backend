package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "events")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_open")
    private Instant dateOpen;

    @Column(name = "date_close")
    private Instant dateClose;

    @Column(name = "date_happen")
    private Instant dateHappen;

    @Column(name = "capacity")
    private Integer capacity = 0;

    @Column(name = "male_quantity")
    private Integer maleQuantity = 0;

    @Column(name = "female_quantity")
    private Integer femaleQuantity = 0;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "ai_screening_result", columnDefinition = "TEXT")
    private String aiScreeningResult;

    @Column(name = "ai_screening_score", precision = 5, scale = 2)
    private BigDecimal aiScreeningScore = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "DRAFT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id")
    private EventTypeJpaEntity eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id")
    private CriteriaJpaEntity criteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fanpage_id")
    private FanpageJpaEntity fanpage;
}
