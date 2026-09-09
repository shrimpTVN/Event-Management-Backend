package com.persistence.repository;

import com.model.User;
import com.persistence.base.AbstractJpaRepository;
import com.persistence.entity.UserJpaEntity;
import com.persistence.mapper.UserMapper;
import com.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {


    @Override
    public User findByUsername(String username) {
        if (username.equals("shrimp")){
            User user = new User();
            user.setId(1L);
            user.setUsername("shrimp");
            user.setPassword("123123");
            user.setEmail("shrimp@example.com");
            user.setRole("USER");
            return user;
        }
        return null;
    }

    @Override
    public int save(User user) {
        // Implement the save logic here
        return 0;
    }


}