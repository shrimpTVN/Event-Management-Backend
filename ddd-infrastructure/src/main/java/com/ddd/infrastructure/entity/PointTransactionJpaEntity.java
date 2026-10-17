package com.ddd.infrastructure.entity;

import com.ddd.domain.enums.PointTransactionStatusEnum;
import com.ddd.domain.enums.PointTransactionTypeEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "point_transactions")
public class PointTransactionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "semester_id", nullable = false)
    private SemesterJpaEntity semester;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "point_category_id", nullable = false)
    private PointCategoryJpaEntity pointCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "event_id")
    private EventJpaEntity event;

    @NotNull
    @Column(name = "points", nullable = false)
    private Integer points;

    @Size(max = 50)
    @NotNull
    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PointTransactionTypeEnum type=PointTransactionTypeEnum.MANUAL;

    @Enumerated(EnumType.STRING)
    @Size(max = 50)
    @NotNull
    @ColumnDefault("'CONFIRMED'")
    @Column(name = "status", nullable = false, length = 50)
    private PointTransactionStatusEnum status=PointTransactionStatusEnum.CONFIRMED;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;


}