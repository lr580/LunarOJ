package com.lunaroj;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "local"})
class JwtSecretPrintTest {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Test
    void shouldReadAndPrintJwtSecret() {
        System.out.println("app.security.jwt.secret=" + jwtSecret); // 运行以检验当前配置的 secret 是否符合预期
        assertThat(jwtSecret).isNotBlank();
    }
}
