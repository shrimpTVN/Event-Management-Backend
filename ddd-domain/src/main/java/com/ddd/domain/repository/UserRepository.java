package com.ddd.domain.repository;

import com.ddd.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository {
    User findByEmail(String email);
    List<User> findAll();
    Boolean existsByEmail(String email);
    User save(User user, Set<String> roleNames);

    default User save(User user, String roleName) {
        return save(user, Set.of(roleName));
    }

    User save(User user);

    default User update(User user) {
        return save(user);
    }
    Optional<User> findById(Long userId);
}

