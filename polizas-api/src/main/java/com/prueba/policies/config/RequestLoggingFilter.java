package com.prueba.policies.config;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long started = System.nanoTime();
        response.setHeader("X-Request-Id", requestId);
        // Ignore client-provided IDs: they are untrusted and must not enter the log context.
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", requestId)) {
            boolean failed = false;
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException | Error ex) {
                failed = true;
                throw ex;
            } finally {
                log.atInfo().addKeyValue("method", request.getMethod())
                        .addKeyValue("status", failed ? 500 : response.getStatus())
                        .addKeyValue("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started))
                        .log("http.request.completed");
            }
        }
    }
}
