package com.lunaroj.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.lunaroj.model.vo.CaptchaVO;
import com.lunaroj.service.CaptchaService;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.Font;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final int CAPTCHA_WIDTH = 180;
    private static final int CAPTCHA_HEIGHT = 64;
    private static final int CAPTCHA_CODE_COUNT = 4;
    private static final int CAPTCHA_INTERFERE_LINE_COUNT = 45;
    private static final String[] CAPTCHA_FONT_FAMILIES = {
            "Times New Roman",
            "Georgia",
            "Cambria",
            "Consolas",
            "Serif"
    };
    private static final int CAPTCHA_FONT_MIN_SIZE = 42;
    private static final int CAPTCHA_FONT_MAX_SIZE = 48;

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO generateCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH,
                CAPTCHA_HEIGHT,
                CAPTCHA_CODE_COUNT,
                CAPTCHA_INTERFERE_LINE_COUNT
        );
        captcha.setFont(randomComplexFont());
        String code = captcha.getCode().toLowerCase(Locale.ROOT);

        String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
        stringRedisTemplate.opsForValue().set(captchaKey, code, CAPTCHA_TTL);

        return new CaptchaVO(captchaId, captcha.getImageBase64Data());
    }

    @Override
    public void verifyCaptcha(String captchaId, String captchaCode) {
        String captchaKey = CAPTCHA_KEY_PREFIX + captchaId;
        String storedCode = stringRedisTemplate.opsForValue().get(captchaKey);
        if (!StringUtils.hasText(storedCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        if (!storedCode.equalsIgnoreCase(captchaCode)) {
            stringRedisTemplate.delete(captchaKey);
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        stringRedisTemplate.delete(captchaKey);
    }

    private Font randomComplexFont() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        String family = CAPTCHA_FONT_FAMILIES[random.nextInt(CAPTCHA_FONT_FAMILIES.length)];
        int style = Font.BOLD;
        if (random.nextBoolean()) {
            style |= Font.ITALIC;
        }
        int size = random.nextInt(CAPTCHA_FONT_MIN_SIZE, CAPTCHA_FONT_MAX_SIZE + 1);
        return new Font(family, style, size);
    }
}





