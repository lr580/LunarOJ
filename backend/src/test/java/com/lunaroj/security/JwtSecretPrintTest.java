package com.lunaroj.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "local"})
@Tag("integration")
class JwtSecretPrintTest {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Test
    void shouldReadJwtSecret() {
        assertThat(jwtSecret).isNotBlank();
    }
}

