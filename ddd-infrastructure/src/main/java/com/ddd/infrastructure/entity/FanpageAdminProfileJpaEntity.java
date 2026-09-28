package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "fanpage_admin_profiles")
public class FanpageAdminProfileJpaEntity extends BaseEntityJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;

    @Size(max = 20)
    @NotNull
    @Column(name = "staff_id", nullable = false, length = 20)
    private String staffId;

    @Size(max = 20)
    @Column(name = "org_name", length = 20)
    private String orgName;

    @Size(max = 100)
    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "dob")
    private LocalDate dob;

    @Size(max = 10)
    @Column(name = "gender", length = 10)
    private String gender;

    @Size(max = 20)
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_url", length = Integer.MAX_VALUE)
    private String avatarUrl;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;


}