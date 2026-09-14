package com.ddd.application.service.auth;

import com.ddd.application.dto.auth.LoginDto;
import com.ddd.application.dto.auth.UserRegisterDto;

public interface AuthService {
    public LoginDto login(String username, String password);
    public void createUser(UserRegisterDto userRegisterRequest);
}
