package com.ddd.infrastructure.entity;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class NotificationJpaEntity extends BaseEntityJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private NotificationTypeEnum type;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatusEnum status = NotificationStatusEnum.UNREAD;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private UserJpaEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fanpage_id", nullable = true)
    private FanpageJpaEntity fanpage;
}
