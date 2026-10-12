package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criterias")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CriteriaJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "children_order", nullable = false)
    private Integer childrenOrder = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CriteriaJpaEntity parent;
}
