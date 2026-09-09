package com.service.auth.impl;

import com.dto.auth.LoginDto;
import com.model.User;
import com.repository.UserRepository;
import com.service.auth.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;

    @Override
    public LoginDto login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Credentials don't match");
        }
        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Credentials don't match");
        }

        return new LoginDto(user.getId(), "access_token", "user");
    }
}
