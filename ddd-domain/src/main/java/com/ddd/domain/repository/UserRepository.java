package com.ddd.domain.repository;

import com.ddd.domain.model.User;

public interface UserRepository {
    User findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    void save(User user);
}
