package com.ddd.domain.repository;

import com.ddd.domain.model.User;

import java.util.List;

public interface UserRepository {
    User findByEmail(String email);
    List<User> findAll();
    Boolean existsByEmail(String email);
    User save(User user);

}
