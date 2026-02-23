package com.lunaroj.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokenVO {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
}




