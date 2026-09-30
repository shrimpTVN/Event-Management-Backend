package com.ddd.infrastructure.entity;

import com.ddd.domain.enums.EventStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, columnDefinition = "TEXT")
    private String location;

    @Column(name = "date_open", nullable = false)
    private Instant dateOpen;

    @Column(name = "date_close", nullable = false)
    private Instant dateClose;

    @Column(name = "date_happen", nullable = false)
    private Instant dateHappen;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 0;

    @Column(name = "male_quantity")
    private Integer maleQuantity = 0;

    @Column(name = "female_quantity")
    private Integer femaleQuantity = 0;

    @Column(name = "banner_url", nullable = false, columnDefinition = "TEXT")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false, length = 50)
    private EventStatusEnum status = EventStatusEnum.DRAFT;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 10) // Instructs Hibernate to fetch eventPoints in batches of 10
    private List<EventPointJpaEntity> eventPoints = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_type_id")
    private EventTypeJpaEntity eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id")
    private CriteriaJpaEntity criteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fanpage_id")
    private FanpageJpaEntity fanpage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private SemesterJpaEntity semester;
}
