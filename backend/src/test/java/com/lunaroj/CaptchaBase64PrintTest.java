package com.lunaroj;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaBase64PrintTest {

    @Test
    void shouldPrintCaptchaBase64Data() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(180, 64, 5, 45);
        String base64Data = captcha.getImageBase64Data();

        System.out.println("captcha.code=" + captcha.getCode());
        System.out.println("captcha.getImageBase64Data()=" + base64Data);

        assertThat(base64Data).isNotBlank();
    }
}
