package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "fanpages")
public class FanpageJpaEntity extends BaseEntityJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "name")
    private String name;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

    @Size(max = 50)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Size(max = 100)
    @NotNull
    @Column(name = "org_type", nullable = false, length = 100)
    private String orgType;

    @Size(max = 200)
    @NotNull
    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    @Size(max = 255)
    @NotNull
    @Column(name = "parent_org", nullable = false)
    private String parentOrg;

    @Size(max = 255)
    @Column(name = "help_contact")
    private String helpContact;

    @Size(max = 20)
    @Column(name = "help_phone", length = 20)
    private String helpPhone;


}