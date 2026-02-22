package com.lunaroj.auth.dto.req;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
