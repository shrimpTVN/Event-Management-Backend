package com.service.auth;

import com.dto.auth.LoginDto;

public interface LoginService {
    public LoginDto login(String username, String password);
}
