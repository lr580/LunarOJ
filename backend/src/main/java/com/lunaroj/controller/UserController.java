package com.lunaroj.controller;

import com.lunaroj.model.dto.ChangePasswordDTO;
import com.lunaroj.model.dto.UpdateUserBasicDTO;
import com.lunaroj.model.dto.UpdateUserProfileDTO;
import com.lunaroj.model.vo.UserPublicProfileVO;
import com.lunaroj.model.vo.UserProfileVO;
import com.lunaroj.security.JwtUserPrincipal;
import com.lunaroj.service.AuthService;
import com.lunaroj.service.UserProfileService;
import com.lunaroj.common.response.ApiResponse;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
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
    public ApiResponse<UserPublicProfileVO> getUserPublicProfile(@PathVariable String username) {
        return ApiResponse.success(userProfileService.getUserPublicProfileByUsername(username));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me(@AuthenticationPrincipal JwtUserPrincipal principal) {
        return ApiResponse.success(userProfileService.getCurrentUserProfile(requireUserId(principal)));
    }

    @PutMapping("/me/basic")
    public ApiResponse<Void> updateBasic(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateUserBasicDTO request
    ) {
        userProfileService.updateCurrentUserBasic(requireUserId(principal), request);
        return ApiResponse.success();
    }

    @PutMapping("/me/profile")
    public ApiResponse<Void> updateProfile(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @Valid @RequestBody UpdateUserProfileDTO request
    ) {
        userProfileService.updateCurrentUserProfile(requireUserId(principal), request);
        return ApiResponse.success();
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ChangePasswordDTO request
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





