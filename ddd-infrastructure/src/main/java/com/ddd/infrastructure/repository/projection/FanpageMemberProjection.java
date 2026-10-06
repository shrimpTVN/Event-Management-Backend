package com.ddd.infrastructure.repository.projection;

/**
 * Spring Data JPA projection for querying fanpage members with user profile details.
 */
public interface FanpageMemberProjection {
    Long getUserId();
    String getEmail();
    String getFirstName();
    String getLastName();
    String getAvatarUrl();
    String getRole();
}
