package com.lunaroj.auth.controller;

import com.lunaroj.auth.dto.req.LoginRequest;
import com.lunaroj.auth.dto.req.LogoutRequest;
import com.lunaroj.auth.dto.req.RefreshTokenRequest;
import com.lunaroj.auth.dto.req.RegisterRequest;
import com.lunaroj.auth.dto.resp.AuthTokenResponse;
import com.lunaroj.auth.dto.resp.CaptchaResponse;
import com.lunaroj.auth.service.AuthService;
import com.lunaroj.auth.service.CaptchaService;
import com.lunaroj.auth.service.SystemConfigService;
import com.lunaroj.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final SystemConfigService systemConfigService;

    @GetMapping("/register-enabled")
    public ApiResponse<Boolean> registerEnabled() {
        return ApiResponse.success(systemConfigService.isRegisterEnabled());
    }

    @GetMapping("/captcha")
    public ApiResponse<CaptchaResponse> captcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(authorizationHeader, refreshToken);
        return ApiResponse.success();
    }
}
