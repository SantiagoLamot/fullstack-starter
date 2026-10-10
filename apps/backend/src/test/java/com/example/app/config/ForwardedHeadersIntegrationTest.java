package com.example.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.app.support.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Real Tomcat (RemoteIpValve): a client that is NOT a trusted proxy cannot pick its IP with X-Forwarded-For,
 * so rotating the header does not evade the per-IP limit. Here the loopback peer is deliberately untrusted
 * (in production only private/Docker addresses, i.e. our nginx, are).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ForwardedHeadersIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void spoofedForwardedForFromAnUntrustedClientDoesNotEvadeThePerIpLimit() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        for (int i = 1; i <= 6; i++) {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create("http://localhost:" + port + "/api/v1/auth/password-reset/request"))
                    .header("Content-Type", "application/json")
                    .header("X-Forwarded-For", "203.0.113." + i)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"email\":\"xff-" + UUID.randomUUID() + "@example.test\"}"))
                    .build();
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            assertThat(status).as("request %d", i).isEqualTo(i <= 5 ? 204 : 429);
        }
    }
}
