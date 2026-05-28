package com.GestorProyectos.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple IP-based rate limiter for sensitive auth endpoints.
 * Uses a Redis counter with a 15-minute sliding window.
 *
 * Limits:
 *   /api/auth/login          → 10 attempts per IP per 15 min
 *   /api/auth/forgot-password → 5  attempts per IP per 15 min
 *   /api/auth/register        → 5  attempts per IP per 15 min
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int  WINDOW_SECONDS  = 900; // 15 minutes
    private static final int  LOGIN_LIMIT      = 10;
    private static final int  SENSITIVE_LIMIT  = 5;

    @Autowired
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.equals("/api/auth/login")
            && !path.equals("/api/auth/forgot-password")
            && !path.equals("/api/auth/register");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String ip   = resolveClientIp(request);
        String path = request.getRequestURI();
        int limit   = path.equals("/api/auth/login") ? LOGIN_LIMIT : SENSITIVE_LIMIT;

        String key  = "ratelimit:" + path + ":" + ip;
        Long count  = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
