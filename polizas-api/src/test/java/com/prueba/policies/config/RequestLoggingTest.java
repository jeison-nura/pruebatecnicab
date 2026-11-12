package com.prueba.policies.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void logsJsonWithCorrelationAndWithoutCredentialsOrQueryString(CapturedOutput output) throws Exception {
        String secret = "do-not-log-this-credential";
        var result = mvc.perform(get("/v1/polizas?search=private-value")
                        .header("x-api-key", secret).header("X-Request-Id", "client-value"))
                .andExpect(status().isUnauthorized()).andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        assertThat(requestId).isNotBlank().isNotEqualTo("client-value");
        JsonNode event = Arrays.stream(output.getOut().split("\\R"))
                .filter(line -> line.startsWith("{"))
                .map(line -> {
                    try {
                        return mapper.readTree(line);
                    } catch (Exception e) {
                        throw new AssertionError("Log line is not JSON: " + line, e);
                    }
                })
                .filter(node -> "http.request.completed".equals(node.path("message").asText()))
                .findFirst().orElseThrow();
        assertThat(event.path("requestId").asText()).isEqualTo(requestId);
        assertThat(event.path("method").asText()).isEqualTo("GET");
        assertThat(event.path("status").asInt()).isEqualTo(401);
        assertThat(event.path("durationMs").asLong()).isGreaterThanOrEqualTo(0);
        assertThat(event.toString()).doesNotContain(secret, "private-value", "client-value");
    }

    @Test
    void logsValidationErrorCodeButNeverRequestBody(CapturedOutput output) throws Exception {
        String privatePayload = "very-private-risk-description";
        var result = mvc.perform(post("/v1/polizas/{id}/riesgos", UUID.randomUUID())
                        .header("x-api-key", "123456")
                        .contentType("application/json")
                        .content("{\"description\":\"  \" ,\"secret\":\"" + privatePayload + "\"}"))
                .andExpect(status().isBadRequest()).andReturn();

        JsonNode error = event(output, "http.request.error");
        assertThat(error.path("errorCode").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.path("requestId").asText()).isEqualTo(result.getResponse().getHeader("X-Request-Id"));
        assertThat(output.getOut()).doesNotContain(privatePayload);
    }

    @Test
    void duplicateIdempotencyKeyDoesNotLeakIntoSqlWarning(CapturedOutput output) throws Exception {
        String key = "do-not-log-idempotency-key-987";
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", key).contentType("application/json")
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}"))
                .andExpect(status().isOk());
        mvc.perform(post("/core-mock/evento").header("x-api-key", "123456")
                        .header("Idempotency-Key", key).contentType("application/json")
                        .content("{\"evento\":\"ACTUALIZACION\",\"polizaId\":556}"))
                .andExpect(status().isConflict());

        assertThat(output.getOut()).doesNotContain(key);
    }

    private JsonNode event(CapturedOutput output, String message) {
        return Arrays.stream(output.getOut().split("\\R"))
                .filter(line -> line.startsWith("{"))
                .map(mapper::readTree)
                .filter(node -> message.equals(node.path("message").asText()))
                .findFirst().orElseThrow();
    }
}
