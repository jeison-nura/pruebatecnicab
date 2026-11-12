package com.prueba.policies.config;

import com.prueba.policies.domain.ApiIdempotency;
import com.prueba.policies.repository.ApiIdempotencyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Enumeration;

@Component
@Order(1) // runs after ApiKeyFilter (which has no @Order = runs last)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int TTL_HOURS = 24;

    private final ApiIdempotencyRepository idempotencyRepository;

    public IdempotencyFilter(ApiIdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply to mutating methods
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

        // Read and cache request body
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestBody = StreamUtils.copyToString(cachedRequest.getInputStream(), StandardCharsets.UTF_8);

        // Compute request hash (method + path + body)
        String requestHash = hash(request.getMethod() + "|" + request.getRequestURI() + "|" + requestBody);

        // Check for existing idempotency record
        var existing = idempotencyRepository.findByIdempotencyKeyAndRequestHash(idempotencyKey, requestHash);
        if (existing.isPresent()) {
            ApiIdempotency record = existing.get();
            if (!record.isExpired()) {
                // Return cached response
                log.debug("Idempotency hit for key: {}", idempotencyKey);
                response.setStatus(record.getResponseStatus());
                response.setContentType("application/json");
                if (record.getResponseBody() != null) {
                    response.getWriter().write(record.getResponseBody());
                }
                return;
            } else {
                // Expired - delete and continue
                idempotencyRepository.delete(record);
            }
        }

        // No cache hit - proceed with request, capture response
        CachedBodyHttpServletResponse cachedResponse = new CachedBodyHttpServletResponse(response);
        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            // Store response in cache (only on success - 2xx)
            int status = cachedResponse.getStatus();
            if (status >= 200 && status < 300) {
                String responseBody = cachedResponse.getBodyAsString();
                ApiIdempotency record = new ApiIdempotency(
                        idempotencyKey,
                        requestHash,
                        request.getMethod(),
                        request.getRequestURI(),
                        status,
                        responseBody,
                        LocalDateTime.now().plusHours(TTL_HOURS)
                );
                idempotencyRepository.save(record);
                log.debug("Idempotency stored for key: {}", idempotencyKey);
            }
            // Copy cached response to actual response
            cachedResponse.copyTo(response);
        }
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Wrapper to cache request body so it can be read multiple times. */
    private static class CachedBodyHttpServletRequest extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] cachedBody;

        public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public java.io.BufferedReader getReader() {
            return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }

    /** ServletInputStream that reads from a byte array. */
    private static class CachedServletInputStream extends ServletInputStream {
        private final java.io.ByteArrayInputStream buffer;

        public CachedServletInputStream(byte[] cachedBody) {
            this.buffer = new java.io.ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() { return buffer.available() == 0; }
        @Override
        public boolean isReady() { return true; }
        @Override
        public void setReadListener(ReadListener listener) {}
        @Override
        public int read() { return buffer.read(); }
    }

    /** Wrapper to capture response body. */
    private static class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final ServletOutputStreamWrapper outputStream;
        private final java.io.PrintWriter writer;

        public CachedBodyHttpServletResponse(HttpServletResponse response) {
            super(response);
            this.outputStream = new ServletOutputStreamWrapper(buffer);
            this.writer = new java.io.PrintWriter(outputStream, true, StandardCharsets.UTF_8);
        }

        @Override
        public ServletOutputStream getOutputStream() { return outputStream; }
        @Override
        public java.io.PrintWriter getWriter() { return writer; }

        public String getBodyAsString() {
            writer.flush();
            return buffer.toString(StandardCharsets.UTF_8);
        }

        public void copyTo(HttpServletResponse response) throws IOException {
            writer.flush();
            response.setStatus(getStatus());
            // Copy headers
            for (String header : getHeaderNames()) {
                for (String value : getHeaders(header)) {
                    response.addHeader(header, value);
                }
            }
            // Copy body
            if (buffer.size() > 0) {
                response.setContentLength(buffer.size());
                response.getOutputStream().write(buffer.toByteArray());
            }
        }

        private static class ServletOutputStreamWrapper extends ServletOutputStream {
            private final ByteArrayOutputStream buffer;
            public ServletOutputStreamWrapper(ByteArrayOutputStream buffer) { this.buffer = buffer; }
            @Override public boolean isReady() { return true; }
            @Override public void setWriteListener(WriteListener listener) {}
            @Override public void write(int b) { buffer.write(b); }
            @Override public void write(byte[] b, int off, int len) { buffer.write(b, off, len); }
        }
    }
}