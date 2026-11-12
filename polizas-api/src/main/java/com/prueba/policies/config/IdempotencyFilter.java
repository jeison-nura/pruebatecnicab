package com.prueba.policies.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.prueba.policies.domain.ApiIdempotency;
import com.prueba.policies.service.IdempotencyService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private final IdempotencyService idempotencyService;

    public IdempotencyFilter(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only mutating HTTP methods are eligible for idempotency.
        String method = request.getMethod();
        return !("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (idempotencyKey.length() > 64) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Idempotency-Key must be at most 64 characters");
            return;
        }

        CachedBodyRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyRequest(request);
        } catch (IllegalArgumentException oversized) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, oversized.getMessage());
            return;
        }
        String requestBody = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);

        // A key cannot be reused across methods, paths or bodies.
        String requestHash = hash(request.getMethod() + "|" + request.getRequestURI() + "|" + requestBody);

        IdempotencyService.Claim claim = idempotencyService.claim(
                idempotencyKey, requestHash, request.getMethod(), request.getRequestURI());
        if (claim.outcome() == IdempotencyService.Outcome.CONFLICT) {
            response.sendError(HttpServletResponse.SC_CONFLICT, "Idempotency-Key already used for another request");
            return;
        }
        if (claim.outcome() == IdempotencyService.Outcome.IN_PROGRESS) {
            response.setHeader("Retry-After", "1");
            response.sendError(HttpServletResponse.SC_CONFLICT, "Request is still in progress");
            return;
        }
        if (claim.outcome() == IdempotencyService.Outcome.REPLAY) {
            replay(claim.response(), response);
            return;
        }

        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
            int status = cachedResponse.getStatus();
            if (status >= 200 && status < 300) {
                String responseBody = new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                idempotencyService.complete(idempotencyKey, requestHash, status, responseBody);
                log.debug("Idempotency stored");
            } else if (status >= 400 && status < 500) {
                // Client errors have not committed the requested operation; let the caller correct it.
                idempotencyService.release(idempotencyKey, requestHash);
            }
        } finally {
            cachedResponse.copyBodyToResponse();
        }
    }

    private void replay(ApiIdempotency record, HttpServletResponse response) throws IOException {
        log.debug("Idempotency hit");
        response.setStatus(record.getResponseStatus());
        response.setContentType("application/json");
        if (record.getResponseBody() != null) {
            response.getWriter().write(record.getResponseBody());
        }
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
