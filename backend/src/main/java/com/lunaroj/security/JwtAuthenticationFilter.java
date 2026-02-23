package com.lunaroj.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunaroj.common.response.ApiResponse;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/problems",
            "/api/problem-sets",
            "/api/groups",
            "/api/rankings",
            "/api/contests",
            "/api/announcements",
            "/api/solutions"
    );
    private static final Pattern PUBLIC_USER_PROFILE_PATTERN = Pattern.compile("^/api/users/[^/]+/profile$");

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) {
            return true;
        }
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        if (PUBLIC_USER_PROFILE_PATTERN.matcher(uri).matches()) {
            return true;
        }
        return PUBLIC_GET_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);
        try {
            Claims claims = jwtTokenProvider.parseClaims(token);
            if (!jwtTokenProvider.isAccessToken(claims)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            if (isBlacklisted(claims.getId())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录态已退出");
            }
            Long userId = Long.valueOf(claims.getSubject());
            String username = jwtTokenProvider.getUsername(claims);
            JwtUserPrincipal principal = new JwtUserPrincipal(userId, username);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            writeUnauthorizedResponse(response, ex.getMessage());
        }
    }

    private boolean isBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) {
            return false;
        }
        Boolean hasKey = stringRedisTemplate.hasKey(AUTH_BLACKLIST_KEY_PREFIX + jti);
        return Boolean.TRUE.equals(hasKey);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED, message)));
    }
}


