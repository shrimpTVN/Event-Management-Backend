package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "point_categories")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PointCategoryJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "maximum", nullable = false)
    private Integer maximum = 0;

    @Column(name = "date_apply")
    private LocalDate dateApply;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private PointCategoryJpaEntity parentCategory;
}
