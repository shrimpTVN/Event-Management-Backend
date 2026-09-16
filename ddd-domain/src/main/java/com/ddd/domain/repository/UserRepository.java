package com.ddd.domain.repository;

import com.ddd.domain.model.User;

import java.util.List;

public interface UserRepository {
    User findByUsername(String username);
    List<User> findAll();
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    void save(User user);
    void save(List<User> users);
}
