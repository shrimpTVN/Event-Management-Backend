package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fanpages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FanpageJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";


}
