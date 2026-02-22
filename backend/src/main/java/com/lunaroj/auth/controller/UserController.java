package com.lunaroj.auth.controller;

import com.lunaroj.auth.dto.req.ChangePasswordRequest;
import com.lunaroj.auth.dto.req.UpdateUserBasicRequest;
import com.lunaroj.auth.dto.req.UpdateUserProfileRequest;
import com.lunaroj.auth.dto.resp.UserPublicProfileResponse;
import com.lunaroj.auth.dto.resp.UserProfileResponse;
import com.lunaroj.auth.security.JwtUserPrincipal;
import com.lunaroj.auth.service.AuthService;
import com.lunaroj.auth.service.UserProfileService;
import com.lunaroj.common.ApiResponse;
import com.lunaroj.common.BusinessException;
import com.lunaroj.common.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final AuthService authService;

    @GetMapping("/{username}/profile")
    public ApiResponse<UserPublicProfileResponse> getUserPublicProfile(@PathVariable String username) {
        return ApiResponse.success(userProfileService.getUserPublicProfileByUsername(username));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ApiResponse.success(userProfileService.getCurrentUserProfile(requireUserId(principal)));
    }

    @PutMapping("/me/basic")
    public ApiResponse<Void> updateBasic(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateUserBasicRequest request
    ) {
        userProfileService.updateCurrentUserBasic(requireUserId(principal), request);
        return ApiResponse.success();
    }

    @PutMapping("/me/profile")
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        userProfileService.updateCurrentUserProfile(requireUserId(principal), request);
        return ApiResponse.success();
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(requireUserId(principal), request);
        authService.logout(authorizationHeader, null);
        return ApiResponse.success();
    }

    private Long requireUserId(JwtUserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
