package com.prueba.policies.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "x-api-key";
    private static final String VALID_API_KEY = "123456";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Skip actuator endpoints
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        if (apiKey == null || !VALID_API_KEY.equals(apiKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "timestamp": "%s",
                    "status": 401,
                    "code": "UNAUTHORIZED",
                    "message": "Missing or invalid x-api-key header",
                    "path": "%s"
                }
                """.formatted(java.time.LocalDateTime.now(), path));
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}