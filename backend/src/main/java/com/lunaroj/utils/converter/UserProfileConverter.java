package com.lunaroj.utils.converter;

import com.lunaroj.model.vo.UserProfileVO;
import com.lunaroj.model.vo.UserPublicProfileVO;
import com.lunaroj.model.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileConverter {

    @Mapping(target = "permissionGroupName", ignore = true)
    UserProfileVO toCurrentUserProfileVO(UserEntity userEntity);

    @Mapping(target = "permissionGroupName", ignore = true)
    UserPublicProfileVO toPublicProfileVO(UserEntity userEntity);
}





