package com.lunaroj.controller;

import com.lunaroj.model.dto.LoginDTO;
import com.lunaroj.model.dto.LogoutDTO;
import com.lunaroj.model.dto.RefreshTokenDTO;
import com.lunaroj.model.dto.RegisterDTO;
import com.lunaroj.model.vo.AuthTokenVO;
import com.lunaroj.model.vo.CaptchaVO;
import com.lunaroj.service.AuthService;
import com.lunaroj.service.CaptchaService;
import com.lunaroj.service.SystemConfigService;
import com.lunaroj.common.response.ApiResponse;
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
    public ApiResponse<CaptchaVO> captcha() {
        return ApiResponse.success(captchaService.generateCaptcha());
    }

    @GetMapping("/captcha-expire-seconds")
    public ApiResponse<Long> captchaExpireSeconds() {
        return ApiResponse.success(captchaService.getCaptchaExpireSeconds());
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterDTO request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenVO> login(@Valid @RequestBody LoginDTO request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody(required = false) LogoutDTO request
    ) {
        String refreshToken = request == null ? null : request.getRefreshToken();
        authService.logout(authorizationHeader, refreshToken);
        return ApiResponse.success();
    }
}





