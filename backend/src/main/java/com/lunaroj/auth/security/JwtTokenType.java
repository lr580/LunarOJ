package com.lunaroj.auth.security;

public enum JwtTokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    JwtTokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
