package com.lunaroj.util;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaBase64PrintTest {

    @Test
    void shouldGenerateCaptchaBase64Data() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(180, 64, 5, 45);
        String base64Data = captcha.getImageBase64Data();

        assertThat(base64Data).isNotBlank();
    }
}

