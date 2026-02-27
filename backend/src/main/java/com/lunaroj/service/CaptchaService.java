package com.lunaroj.service;

import com.lunaroj.model.vo.CaptchaVO;

public interface CaptchaService {

    CaptchaVO generateCaptcha();

    void verifyCaptcha(String captchaId, String captchaCode);

    long getCaptchaExpireSeconds();
}




