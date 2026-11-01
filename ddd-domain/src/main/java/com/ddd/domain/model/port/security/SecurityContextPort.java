package com.ddd.domain.model.port.security;

public interface SecurityContextPort {
    Long getCurrentUserId();
    String getCurrentUserEmail();
    boolean hasRole(String role);
    boolean isAuthenticated();
}
