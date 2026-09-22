package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "conversations")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ConversationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fanpage_id", nullable = false)
    private FanpageJpaEntity fanpage;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private Instant createdAt;
}
