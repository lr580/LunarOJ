package com.lunaroj.service;

import com.lunaroj.model.dto.ChangePasswordDTO;
import com.lunaroj.model.dto.UpdateUserBasicDTO;
import com.lunaroj.model.dto.UpdateUserProfileDTO;
import com.lunaroj.model.vo.UserPublicProfileVO;
import com.lunaroj.model.vo.UserProfileVO;

public interface UserProfileService {

    UserProfileVO getCurrentUserProfile(Long userId);

    UserPublicProfileVO getUserPublicProfileByUsername(String username);

    void updateCurrentUserBasic(Long userId, UpdateUserBasicDTO request);

    void updateCurrentUserProfile(Long userId, UpdateUserProfileDTO request);

    void changePassword(Long userId, ChangePasswordDTO request);
}




