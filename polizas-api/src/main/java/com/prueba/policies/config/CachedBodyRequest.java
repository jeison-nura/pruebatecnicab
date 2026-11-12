package com.prueba.policies.config;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/** Allows the idempotency filter and MVC to read the same request body. */
final class CachedBodyRequest extends HttpServletRequestWrapper {

    private static final int MAX_BODY_BYTES = 1024 * 1024;
    private final byte[] body;

    CachedBodyRequest(HttpServletRequest request) throws IOException {
        super(request);
        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            throw new IllegalArgumentException("Request body exceeds 1 MiB");
        }
        body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new IllegalArgumentException("Request body exceeds 1 MiB");
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return input.read();
            }

            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new UnsupportedOperationException("Async request bodies are not supported");
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
