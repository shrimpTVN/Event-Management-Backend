package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class User extends BaseModel {

    private String email;
    private String password;
    private String provider;
    private String firstName;
    private String lastName;
    private String gender;
    private String avatarUrl;

    private Set<Role> roles = new HashSet<>();

    public void addRole(Role role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(Role role) {
        if (this.roles != null && role != null) {
            this.roles.remove(role);
        }
    }

    public boolean hasRole(String roleName) {
        if (this.roles == null || roleName == null) {
            return false;
        }
        return this.roles.stream()
                .anyMatch(r -> roleName.equalsIgnoreCase(r.getName()));
    }

    public Set<String> getRoleNames() {
        if (this.roles == null) {
            return Collections.emptySet();
        }
        return this.roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Backward-compatible helper returning the first role's ID if present.
     */
    public Long getRoleId() {
        if (this.roles == null || this.roles.isEmpty()) {
            return null;
        }
        return this.roles.iterator().next().getId();
    }

    public String getProviderId() {
        return provider;
    }

    public void setProviderId(String providerId) {
        this.provider = providerId;
    }
}
