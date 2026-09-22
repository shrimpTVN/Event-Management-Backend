package com.ddd.infrastructure.repository;

import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.UserMapper;
import com.ddd.infrastructure.repository.jpaRepository.UserJpaRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    @Override
    public User findByEmail(String email) {
        UserJpaEntity userJpaEntity = userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceAccessException("User not found with email: " + email));
        return userMapper.toDomain(userJpaEntity);
    }

    @Override
    public List<User> findAll(){
        return userJpaRepository.findAll().stream().map(userMapper::toDomain).toList();
    }

    @Override
    public Boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }


    @Override
    public void save(User user) {
        userJpaRepository.save(userMapper.toEntity(user));
    }

    @Override
    public void save(List<User> users) {
        userJpaRepository.saveAll(users.stream().map(userMapper::toEntity).toList());
    }
}
