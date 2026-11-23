package com.ddd.application.service.user.impl;

import com.ddd.application.service.user.AccountSecurityQueryService;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.exception.UnauthorizedException;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSecurityQueryServiceImpl implements AccountSecurityQueryService {

    private final SecurityContextPort securityContextPort;
    private final UserRepository userRepository;

    @Override
    public boolean hasPassword() {
        Long currentUserId = securityContextPort.getCurrentUserId();
        if (currentUserId == null) {
            throw new UnauthorizedException("Authentication required to access account security");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        return user.getPassword() != null && !user.getPassword().isBlank();
    }
}
