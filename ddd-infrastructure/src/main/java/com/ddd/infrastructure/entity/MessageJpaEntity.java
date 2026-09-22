package com.ddd.infrastructure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "messages")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MessageJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sender_type", length = 50)
    private String senderType;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "SENT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ConversationJpaEntity conversation;




}
