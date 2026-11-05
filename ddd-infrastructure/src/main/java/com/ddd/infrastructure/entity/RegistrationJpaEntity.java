package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import com.ddd.domain.enums.RegistrationStatusEnum;

@Entity
@Table(name = "registrations")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RegistrationJpaEntity {

    @EmbeddedId
    private RegistrationId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @MapsId("eventId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventJpaEntity event;

    @Column(name = "reg_date", nullable = false)
    private Instant regDate;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_in_evidence_url", columnDefinition = "TEXT")
    private String checkInEvidenceUrl;

    @Positive
    @Column(name = "check_in_by")
    private Long checkInBy;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "check_out_evidence_url", columnDefinition = "TEXT")
    private String checkOutEvidenceUrl;

    @Positive
    @Column(name = "check_out_by")
    private Long checkOutBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RegistrationStatusEnum status = RegistrationStatusEnum.REGISTERED;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onPrePersist() {
        if (regDate == null) {
            regDate = Instant.now();
        }
    }
}
