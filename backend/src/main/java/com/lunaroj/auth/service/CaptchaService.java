package com.lunaroj.auth.service;

import com.lunaroj.auth.dto.resp.CaptchaResponse;

public interface CaptchaService {

    CaptchaResponse generateCaptcha();

    void verifyCaptcha(String captchaId, String captchaCode);
}
