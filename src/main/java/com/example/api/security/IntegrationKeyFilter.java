package com.example.api.security;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * /api/open/** 集成接口的共享密钥校验。
 * MES 调用时带 X-Integration-Key 请求头。
 */
public class IntegrationKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Integration-Key";

    private final String apiKey;

    public IntegrationKeyFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/open/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(HEADER);
        if (!matches(key, apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"status\":false,\"msg\":\"invalid integration key\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean matches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
