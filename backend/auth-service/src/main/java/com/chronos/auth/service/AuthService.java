package com.chronos.auth.service;

import com.chronos.auth.dto.AuthResponse;
import com.chronos.auth.dto.LoginRequest;
import com.chronos.auth.dto.RefreshTokenRequest;
import com.chronos.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
}
