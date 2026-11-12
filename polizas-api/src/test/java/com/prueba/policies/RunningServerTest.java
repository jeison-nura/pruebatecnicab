package com.prueba.policies;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RunningServerTest {

    @LocalServerPort
    private int port;

    @Test
    void startsOnRandomPortAndServesProtectedEndpoints() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest health = request("/actuator/health").build();
        HttpRequest unauthorized = request("/v1/polizas").build();
        HttpRequest authorized = request("/v1/polizas")
                .header("x-api-key", "123456").build();

        assertThat(client.send(health, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        assertThat(client.send(unauthorized, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        assertThat(client.send(authorized, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).GET();
    }
}
