package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserJpaEntity extends BaseEntityJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @NotNull
    @Column(nullable = false)
    @Min(1)
    private int age;

    @Column(nullable = false)
    @NotBlank
    @Size(min=6)
    private String username;

    @Column(nullable = false)
    @NotBlank
    @Size(min=6)
    private String email;

    @Column(nullable = false)
    @NotBlank
    @Size(min=6)
    private String password;

    @Column(nullable = false)
    @NotBlank
    private String role="USER";
}