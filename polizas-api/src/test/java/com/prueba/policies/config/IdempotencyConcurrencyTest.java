package com.prueba.policies.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.prueba.policies.repository.ApiIdempotencyRepository;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootTest
class IdempotencyConcurrencyTest {

    @Autowired IdempotencyFilter filter;
    @Autowired ApiIdempotencyRepository repository;

    @Test
    void simultaneousRequestsExecuteOnceAndReplayTheResult() throws Exception {
        String key = "concurrent-request";
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        try {
            Future<?> first = executor.submit(() -> {
                try {
                    filter.doFilter(request(key), firstResponse, (req, res) -> {
                        executions.incrementAndGet();
                        entered.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("First request timed out");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        res.setContentType("application/json");
                        res.getWriter().write("{\"accepted\":true}");
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            MockHttpServletResponse concurrentResponse = new MockHttpServletResponse();
            filter.doFilter(request(key), concurrentResponse, (req, res) -> executions.incrementAndGet());

            assertThat(concurrentResponse.getStatus()).isEqualTo(409);
            assertThat(concurrentResponse.getHeader("Retry-After")).isEqualTo("1");
            assertThat(executions).hasValue(1);

            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            MockHttpServletResponse replay = new MockHttpServletResponse();
            filter.doFilter(request(key), replay, (req, res) -> executions.incrementAndGet());
            assertThat(replay.getStatus()).isEqualTo(200);
            assertThat(replay.getContentAsString()).isEqualTo("{\"accepted\":true}");
            assertThat(executions).hasValue(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
            repository.deleteById(key);
        }
    }

    @Test
    void serverFailureKeepsClaimPendingBecauseOutcomeIsUnknown() throws Exception {
        String key = "unknown-outcome";
        try {
            MockHttpServletResponse failed = new MockHttpServletResponse();
            filter.doFilter(request(key), failed, (req, res) -> ((HttpServletResponse) res).setStatus(503));
            assertThat(failed.getStatus()).isEqualTo(503);

            MockHttpServletResponse retry = new MockHttpServletResponse();
            filter.doFilter(request(key), retry, (req, res) -> { throw new AssertionError("must not execute twice"); });
            assertThat(retry.getStatus()).isEqualTo(409);
            assertThat(repository.findById(key).orElseThrow().isCompleted()).isFalse();
            repository.deleteExpired(LocalDateTime.now().plusDays(2));
            assertThat(repository.findById(key)).isPresent();
        } finally {
            repository.deleteById(key);
        }
    }

    @Test
    void clientErrorReleasesClaimForCorrectedRetry() throws Exception {
        String key = "corrected-request";
        try {
            MockHttpServletResponse invalid = new MockHttpServletResponse();
            filter.doFilter(request(key), invalid, (req, res) -> ((HttpServletResponse) res).setStatus(400));
            assertThat(invalid.getStatus()).isEqualTo(400);
            assertThat(repository.findById(key)).isEmpty();

            MockHttpServletResponse corrected = new MockHttpServletResponse();
            filter.doFilter(request(key), corrected, (req, res) -> ((HttpServletResponse) res).setStatus(204));
            assertThat(corrected.getStatus()).isEqualTo(204);
            assertThat(repository.findById(key).orElseThrow().isCompleted()).isTrue();
        } finally {
            repository.deleteById(key);
        }
    }

    @Test
    void rejectsOversizedRequestBeforeClaimingOrExecuting() throws Exception {
        String key = "oversized-request";
        MockHttpServletRequest request = request(key);
        request.setContent(new byte[1024 * 1024 + 1]);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { throw new AssertionError("must not execute"); });

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(repository.findById(key)).isEmpty();
    }

    private MockHttpServletRequest request(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/core-mock/evento");
        request.addHeader("Idempotency-Key", key);
        request.setContent("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}".getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
