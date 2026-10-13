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
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;
    private final RoleJpaRepository roleJpaRepository;

    @Override
    public User findByEmail(String email) {
        UserJpaEntity userJpaEntity = userJpaRepository.findByEmail(email);
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
    public User save(User user, Set<String> roleNames) {
        Set<RoleJpaEntity> roleJpaEntities = roleJpaRepository.findByNameIn(roleNames);
        if (roleJpaEntities.isEmpty()) {
            throw new ResourceAccessException("Roles not found: " + roleNames);
        }
        UserJpaEntity userJpaEntity = userMapper.toEntity(user);
        userJpaEntity.setRoles(roleJpaEntities);
        return userMapper.toDomain(userJpaRepository.save(userJpaEntity));
    }

    @Override
    public User save(User user, String roleName) {
        return save(user, java.util.Set.of(roleName));
    }

    @Override
    public User save(User user) {
        UserJpaEntity userJpaEntity = userMapper.toEntity(user);
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            java.util.Set<String> roleNames = user.getRoleNames();
            java.util.Set<RoleJpaEntity> roleJpaEntities = roleJpaRepository.findByNameIn(roleNames);
            userJpaEntity.setRoles(roleJpaEntities);
        }
        return userMapper.toDomain(userJpaRepository.save(userJpaEntity));
    }

    @Override
    public Optional<User> findById(Long userId) {
        return userJpaRepository.findById(userId).map(userMapper::toDomain);
    }

}
