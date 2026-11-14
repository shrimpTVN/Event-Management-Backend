package com.ddd.domain.model;

import com.ddd.domain.enums.InvatitationStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
public class FanpageMemberInvitation {
    private Long id;
    private Long fanpageId;
    private Long userId;
    private String inviterName;
    private InvatitationStatusEnum status;
    private Instant createdAt;
    private Long createdBy;
    private Instant updatedAt;
    private Long updatedBy;

    public FanpageMemberInvitation(Long fanpageId, Long userId, String inviterName) {
        this.fanpageId = fanpageId;
        this.userId = userId;
        this.inviterName = inviterName;
        this.status = InvatitationStatusEnum.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
