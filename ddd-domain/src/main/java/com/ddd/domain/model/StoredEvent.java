package com.ddd.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent extends BaseModel {
    private Long userId;
    private Long eventId;

    public boolean isOwnedBy(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }
}
