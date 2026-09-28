package com.ddd.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class FanpageMemberJpaEntityId implements Serializable {
    private static final long serialVersionUID = -936829970249940622L;
    @NotNull
    @Column(name = "fanpage_id", nullable = false)
    private Long fanpageId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;


}