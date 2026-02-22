package com.lunaroj.auth.service;

import com.lunaroj.auth.dto.req.ChangePasswordRequest;
import com.lunaroj.auth.dto.req.UpdateUserBasicRequest;
import com.lunaroj.auth.dto.req.UpdateUserProfileRequest;
import com.lunaroj.auth.dto.resp.UserPublicProfileResponse;
import com.lunaroj.auth.dto.resp.UserProfileResponse;

public interface UserProfileService {

    UserProfileResponse getCurrentUserProfile(Long userId);

    UserPublicProfileResponse getUserPublicProfileByUsername(String username);

    void updateCurrentUserBasic(Long userId, UpdateUserBasicRequest request);

    void updateCurrentUserProfile(Long userId, UpdateUserProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
