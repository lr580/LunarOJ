package com.lunaroj.security;

import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_PERMISSION_GROUP = "permissionGroup";

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId, String username, String permissionGroup) {
        return createToken(
                userId,
                username,
                permissionGroup,
                JwtTokenType.ACCESS,
                jwtProperties.getAccessTokenExpireSeconds()
        );
    }

    public String createRefreshToken(Long userId, String username, String permissionGroup) {
        return createToken(
                userId,
                username,
                permissionGroup,
                JwtTokenType.REFRESH,
                jwtProperties.getRefreshTokenExpireSeconds()
        );
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    public boolean isAccessToken(Claims claims) {
        return JwtTokenType.ACCESS.getValue().equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return JwtTokenType.REFRESH.getValue().equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    private String createToken(
            Long userId,
            String username,
            String permissionGroup,
            JwtTokenType tokenType,
            long expireSeconds
    ) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_PERMISSION_GROUP, permissionGroup)
                .claim(CLAIM_TOKEN_TYPE, tokenType.getValue())
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}


