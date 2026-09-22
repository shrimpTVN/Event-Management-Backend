package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "semesters")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SemesterJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "UPCOMING";

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = false;
}
