package com.ddd.infrastructure.repository;

import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.UserMapper;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        UserJpaEntity userJpaEntity = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceAccessException("User not found with username: " + username));
        return userMapper.toDomain(userJpaEntity);
    }

    @Override
    public Boolean existsByUsername(String username) {
        return  userJpaRepository.existsByUsername(username);
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }


    @Override
    public void save(User user) {
        userJpaRepository.save(userMapper.toEntity(user));
    }
}
