package com.lunaroj.service;

import com.lunaroj.model.dto.LoginDTO;
import com.lunaroj.model.dto.RegisterDTO;
import com.lunaroj.model.vo.AuthTokenVO;

public interface AuthService {

    void register(RegisterDTO request);

    AuthTokenVO login(LoginDTO request);

    AuthTokenVO refresh(String refreshToken);

    void logout(String authorizationHeader, String refreshToken);
}




