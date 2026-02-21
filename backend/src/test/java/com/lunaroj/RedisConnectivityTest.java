package com.lunaroj;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "local"})
class RedisConnectivityTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldReadAndWriteRedis() {
        String pong = stringRedisTemplate.execute(RedisConnection::ping);
        assertThat(pong).isEqualTo("PONG");

        String key = "test:redis:connectivity:" + UUID.randomUUID();
        String value = "ok";
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));

        String fetched = stringRedisTemplate.opsForValue().get(key);
        assertThat(fetched).isEqualTo(value);
    }
}
