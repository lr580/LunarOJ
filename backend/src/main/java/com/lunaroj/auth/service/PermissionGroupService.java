package com.lunaroj.auth.service;

public interface PermissionGroupService {

    String CACHE_NAME_GROUP_ID_BY_NAME = "permissionGroupIdByName";
    String CACHE_NAME_GROUP_DISPLAY_NAME_BY_ID = "permissionGroupDisplayNameById";
    String CACHE_NAME_GROUP_NAME_BY_ID = "permissionGroupNameById";

    Long getGroupIdByName(String groupName);

    String getGroupDisplayNameById(Long groupId);

    String getGroupNameById(Long groupId);

    void evictGroupIdCache(String groupName);

    void clearPermissionGroupCache();

    @Deprecated
    default void clearGroupIdCache() {
        clearPermissionGroupCache();
    }
}
