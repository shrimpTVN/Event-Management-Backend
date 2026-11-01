package com.ddd.domain.model;

import com.ddd.domain.enums.FollowingStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Following extends BaseModel {
    private Long fanpageId;
    private Long userId;
    private FollowingStatusEnum folStatus = FollowingStatusEnum.FOLLOWING;

    public Following(Long fanpageId, Long userId) {
        this.fanpageId = fanpageId;
        this.userId = userId;
        this.folStatus = FollowingStatusEnum.FOLLOWING;
    }

    public void follow() {
        this.folStatus = FollowingStatusEnum.FOLLOWING;
    }

    public void unfollow() {
        this.folStatus = FollowingStatusEnum.UNFOLLOWED;
    }

    public boolean isFollowing() {
        return FollowingStatusEnum.FOLLOWING.equals(this.folStatus);
    }
}
