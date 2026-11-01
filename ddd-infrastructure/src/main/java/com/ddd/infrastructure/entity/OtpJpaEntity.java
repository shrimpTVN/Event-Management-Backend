package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "otps", indexes = {
        @Index(name = "idx_otp_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expired_at", nullable = false)
    private Instant expiredAt;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

}
