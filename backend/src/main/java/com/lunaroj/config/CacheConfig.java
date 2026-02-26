package com.lunaroj.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.lunaroj.service.PermissionGroupService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PermissionGroupService.CACHE_NAME_GROUP_ID_BY_NAME,
                PermissionGroupService.CACHE_NAME_GROUP_DISPLAY_NAME_BY_ID,
                PermissionGroupService.CACHE_NAME_GROUP_NAME_BY_ID
        );
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .initialCapacity(32)
                        .maximumSize(1024)
                        .expireAfterWrite(Duration.ofHours(6))
        );
        return cacheManager;
    }
}

