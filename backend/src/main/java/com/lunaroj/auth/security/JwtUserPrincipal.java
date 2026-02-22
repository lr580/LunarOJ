package com.lunaroj.auth.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtUserPrincipal {

    private Long userId;
    private String username;
}
