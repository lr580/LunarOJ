package com.lunaroj.service.impl;

import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.model.vo.CaptchaVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaptchaServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl(stringRedisTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void generateCaptchaShouldStoreLowercaseCodeInRedis() {
        CaptchaVO captcha = captchaService.generateCaptcha();

        assertThat(captcha.getCaptchaId()).isNotBlank();
        assertThat(captcha.getImageBase64()).isNotBlank();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).startsWith("auth:captcha:");
        assertThat(valueCaptor.getValue()).isEqualTo(valueCaptor.getValue().toLowerCase());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void verifyCaptchaShouldDeleteKeyWhenVerificationPassed() {
        when(valueOperations.get("auth:captcha:cid")).thenReturn("abcd");

        captchaService.verifyCaptcha("cid", "ABcD");

        verify(stringRedisTemplate).delete("auth:captcha:cid");
    }

    @Test
    void verifyCaptchaShouldDeleteKeyAndThrowWhenCodeMismatch() {
        when(valueOperations.get("auth:captcha:cid")).thenReturn("abcd");

        assertThatThrownBy(() -> captchaService.verifyCaptcha("cid", "efgh"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_INVALID));

        verify(stringRedisTemplate).delete("auth:captcha:cid");
    }

    @Test
    void verifyCaptchaShouldThrowWhenCaptchaMissingOrExpired() {
        when(valueOperations.get("auth:captcha:cid")).thenReturn(null);

        assertThatThrownBy(() -> captchaService.verifyCaptcha("cid", "abcd"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CAPTCHA_INVALID));

        verify(stringRedisTemplate, never()).delete("auth:captcha:cid");
    }
}
