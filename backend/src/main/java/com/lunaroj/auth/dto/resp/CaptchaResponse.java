package com.lunaroj.auth.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private String captchaId;
    private String imageBase64;
}
