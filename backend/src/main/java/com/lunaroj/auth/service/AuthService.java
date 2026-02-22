package com.lunaroj.auth.service;

import com.lunaroj.auth.dto.req.LoginRequest;
import com.lunaroj.auth.dto.req.RegisterRequest;
import com.lunaroj.auth.dto.resp.AuthTokenResponse;

public interface AuthService {

    void register(RegisterRequest request);

    AuthTokenResponse login(LoginRequest request);

    AuthTokenResponse refresh(String refreshToken);

    void logout(String authorizationHeader, String refreshToken);
}
