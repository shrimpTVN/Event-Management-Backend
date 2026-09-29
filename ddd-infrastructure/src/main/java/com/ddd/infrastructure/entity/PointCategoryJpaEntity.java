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

    @Column(name = "note", columnDefinition = "TEXT", nullable = false)
    private String note="";

    @Column(name = "maximum", nullable = false)
    private Integer maximum = 0;

    @Column(name = "date_apply", nullable = false)
    private LocalDate dateApply;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "children_order", nullable = false)
    private Integer childrenOrder = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private PointCategoryJpaEntity parentCategory;
}
