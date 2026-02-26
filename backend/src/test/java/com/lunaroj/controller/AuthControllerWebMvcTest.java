package com.lunaroj.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.exception.GlobalExceptionHandler;
import com.lunaroj.model.dto.LoginDTO;
import com.lunaroj.model.dto.RegisterDTO;
import com.lunaroj.model.vo.AuthTokenVO;
import com.lunaroj.model.vo.CaptchaVO;
import com.lunaroj.security.JwtAuthenticationFilter;
import com.lunaroj.service.AuthService;
import com.lunaroj.service.CaptchaService;
import com.lunaroj.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private CaptchaService captchaService;
    @MockBean
    private SystemConfigService systemConfigService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerEnabledShouldReturnConfigFlag() throws Exception {
        when(systemConfigService.isRegisterEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/auth/register-enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void captchaShouldReturnCaptchaPayload() throws Exception {
        when(captchaService.generateCaptcha()).thenReturn(new CaptchaVO("cid", "base64-data"));

        mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.captchaId").value("cid"))
                .andExpect(jsonPath("$.data.imageBase64").value("base64-data"));
    }

    @Test
    void loginShouldReturnValidationErrorWhenUsernameMissing() throws Exception {
        LoginDTO request = new LoginDTO();
        request.setPassword("123456");
        request.setCaptchaId("cid");
        request.setCaptchaCode("abcd");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
    }

    @Test
    void registerShouldMapBusinessExceptionToApiResponse() throws Exception {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("alice");
        request.setPassword("123456");
        request.setCaptchaId("cid");
        request.setCaptchaCode("abcd");

        doThrow(new BusinessException(ErrorCode.USERNAME_EXISTS))
                .when(authService).register(org.mockito.ArgumentMatchers.any(RegisterDTO.class));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.USERNAME_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USERNAME_EXISTS.getMessage()));
    }

    @Test
    void refreshShouldReturnTokenPayload() throws Exception {
        when(authService.refresh("r-token"))
                .thenReturn(new AuthTokenVO("a-token", "r-token-new", "Bearer", 1800L));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"r-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accessToken").value("a-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("r-token-new"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void refreshShouldReturnValidationErrorWhenRefreshTokenMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("refreshToken 不能为空"));
    }

    @Test
    void loginShouldReturnBadRequestWhenRequestBodyMalformed() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void captchaShouldMapUnhandledExceptionToInternalError() throws Exception {
        when(captchaService.generateCaptcha()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    @Test
    void logoutShouldSupportEmptyBodyAndNullAuthorization() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(authService).logout(isNull(), isNull());
    }
}
