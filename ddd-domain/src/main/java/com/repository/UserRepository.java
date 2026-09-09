package com.repository;

import com.model.User;

public interface UserRepository {
    User findByUsername(String username);
    int save(User user);
}
