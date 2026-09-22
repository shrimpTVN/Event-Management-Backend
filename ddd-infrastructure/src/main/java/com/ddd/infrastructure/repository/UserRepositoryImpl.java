package com.ddd.infrastructure.repository;

import com.ddd.domain.enums.RoleEnum;
import com.ddd.domain.model.User;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import com.ddd.infrastructure.mapper.UserMapper;
import com.ddd.infrastructure.repository.jpaRepository.RoleJpaRepository;
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
    private final RoleJpaRepository roleJpaRepository;
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
        RoleJpaEntity roleJpaEntity = roleJpaRepository.findByName(RoleEnum.STUDENT.name())
                .orElseThrow(() -> new ResourceAccessException("Role not found with name: " + RoleEnum.STUDENT.name()));
        UserJpaEntity userJpaEntity = userMapper.toEntity(user);
        userJpaEntity.setRole(roleJpaEntity);
        userJpaRepository.save(userJpaEntity);
    }

}
