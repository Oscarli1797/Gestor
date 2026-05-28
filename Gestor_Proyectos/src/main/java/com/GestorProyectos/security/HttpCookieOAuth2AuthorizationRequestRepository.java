package com.GestorProyectos.security;

import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Stores the OAuth2 authorization request in Redis (keyed by state parameter).
 * Only the opaque state value is placed in a short-lived HttpOnly cookie.
 *
 * This replaces the previous Java-serialization-based approach which was
 * vulnerable to deserialization attacks (CWE-502).
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "oauth2_state";
    private static final int   TTL_SECONDS = 180;
    private static final String REDIS_PREFIX = "oauth2:state:";

    @Autowired
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = readStateCookie(request);
        if (state == null) return null;
        return (OAuth2AuthorizationRequest) redisTemplate.opsForValue()
                .get(REDIS_PREFIX + state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        String state = authorizationRequest.getState();
        redisTemplate.opsForValue().set(
                REDIS_PREFIX + state, authorizationRequest, TTL_SECONDS, TimeUnit.SECONDS);

        boolean secure = request.isSecure();
        Cookie cookie = new Cookie(COOKIE_NAME, state);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setMaxAge(TTL_SECONDS);
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizationRequest req = loadAuthorizationRequest(request);
        String state = readStateCookie(request);
        if (state != null) {
            redisTemplate.delete(REDIS_PREFIX + state);
        }
        deleteCookie(request, response);
        return req;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String readStateCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null) return;
        for (Cookie c : request.getCookies()) {
            if (COOKIE_NAME.equals(c.getName())) {
                Cookie blank = new Cookie(c.getName(), "");
                blank.setPath("/");
                blank.setMaxAge(0);
                response.addCookie(blank);
            }
        }
    }
}
