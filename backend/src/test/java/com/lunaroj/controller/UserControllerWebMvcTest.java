package com.lunaroj.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.GlobalExceptionHandler;
import com.lunaroj.model.vo.UserProfileVO;
import com.lunaroj.model.vo.UserPublicProfileVO;
import com.lunaroj.security.JwtAuthenticationFilter;
import com.lunaroj.security.JwtUserPrincipal;
import com.lunaroj.service.AuthService;
import com.lunaroj.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService userProfileService;
    @MockBean
    private AuthService authService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getUserPublicProfileShouldReturnPayload() throws Exception {
        UserPublicProfileVO vo = new UserPublicProfileVO();
        vo.setId(2L);
        vo.setUsername("bob");
        vo.setNickname("B");
        vo.setPermissionGroupName("普通用户");
        vo.setProfile("hello");

        when(userProfileService.getUserPublicProfileByUsername("bob")).thenReturn(vo);

        mockMvc.perform(get("/api/users/bob/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("bob"))
                .andExpect(jsonPath("$.data.permissionGroupName").value("普通用户"));
    }

    @Test
    void getUserPublicProfileShouldMapUnhandledExceptionToInternalError() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(userProfileService).getUserPublicProfileByUsername("bob");

        mockMvc.perform(get("/api/users/bob/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
    }

    @Test
    void getMeShouldReturnUnauthorizedCodeWhenPrincipalMissing() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    void getMeShouldReturnCurrentUserProfileWhenPrincipalPresent() throws Exception {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(1L);
        vo.setUsername("alice");
        vo.setNickname("Alice");
        vo.setPermissionGroupName("普通用户");

        when(userProfileService.getCurrentUserProfile(1L)).thenReturn(vo);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(buildAuthentication(1L, "alice"));
        SecurityContextHolder.setContext(context);
        try {
            mockMvc.perform(get("/api/users/me")
                            .with(authentication(buildAuthentication(1L, "alice"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.username").value("alice"))
                    .andExpect(jsonPath("$.data.permissionGroupName").value("普通用户"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void updateBasicShouldReturnValidationErrorForBadEmail() throws Exception {
        String payload = """
                {"email":"bad-email"}
                """;

        mockMvc.perform(put("/api/users/me/basic")
                        .with(authentication(buildAuthentication(1L, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
    }

    @Test
    void updateBasicShouldReturnBadRequestWhenRequestBodyMalformed() throws Exception {
        mockMvc.perform(put("/api/users/me/basic")
                        .with(authentication(buildAuthentication(1L, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.BAD_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }

    @Test
    void updateProfileShouldReturnValidationErrorWhenProfileIsNull() throws Exception {
        mockMvc.perform(put("/api/users/me/profile")
                        .with(authentication(buildAuthentication(1L, "alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("个人主页内容不能为空，可传空字符串表示清空"));
    }

    @Test
    void changePasswordShouldCallServiceAndLogoutCurrentAccessToken() throws Exception {
        String payload = objectMapper.writeValueAsString(new ChangePasswordPayload("old123456", "new123456"));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(buildAuthentication(1L, "alice"));
        SecurityContextHolder.setContext(context);
        try {
            mockMvc.perform(put("/api/users/me/password")
                            .with(authentication(buildAuthentication(1L, "alice")))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        } finally {
            SecurityContextHolder.clearContext();
        }

        verify(userProfileService).changePassword(eq(1L), any());
        verify(authService).logout(eq("Bearer access-token"), isNull());
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(Long userId, String username) {
        JwtUserPrincipal principal = new JwtUserPrincipal(userId, username);
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    private record ChangePasswordPayload(String oldPassword, String newPassword) {
    }

}
