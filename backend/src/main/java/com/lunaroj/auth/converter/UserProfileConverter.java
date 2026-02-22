package com.lunaroj.auth.converter;

import com.lunaroj.auth.dto.resp.UserProfileResponse;
import com.lunaroj.auth.dto.resp.UserPublicProfileResponse;
import com.lunaroj.auth.entity.UserEntity;
import com.lunaroj.auth.service.PermissionGroupService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserProfileConverter {

    @Mapping(target = "permissionGroupName", ignore = true)
    UserProfileResponse toCurrentUserProfileResponse(UserEntity userEntity, @Context PermissionGroupService permissionGroupService);

    @Mapping(target = "permissionGroupName", ignore = true)
    UserPublicProfileResponse toPublicProfileResponse(UserEntity userEntity, @Context PermissionGroupService permissionGroupService);

    @AfterMapping
    default void fillCurrentUserPermissionGroupName(
            UserEntity source,
            @MappingTarget UserProfileResponse target,
            @Context PermissionGroupService permissionGroupService
    ) {
        target.setPermissionGroupName(permissionGroupService.getGroupDisplayNameById(source.getPermissionGroupId()));
    }

    @AfterMapping
    default void fillPublicUserPermissionGroupName(
            UserEntity source,
            @MappingTarget UserPublicProfileResponse target,
            @Context PermissionGroupService permissionGroupService
    ) {
        target.setPermissionGroupName(permissionGroupService.getGroupDisplayNameById(source.getPermissionGroupId()));
    }
}
