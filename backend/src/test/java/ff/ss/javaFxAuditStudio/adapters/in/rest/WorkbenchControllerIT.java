package ff.ss.javaFxAuditStudio.adapters.in.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkbenchControllerIT {

    @Value("${local.server.port}")
    private int port;

    @Test
    void shouldExposeWorkbenchOverview() throws Exception {
        HttpClient client;
        HttpRequest request;
        HttpResponse<String> response;

        client = HttpClient.newHttpClient();
        request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/workbench/overview"))
                .GET()
                .build();

        response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("JavaFX Audit Studio");
        assertThat(response.body()).contains("Angular 21.2.x");
        assertThat(response.body()).contains("JDK 21 / Spring Boot 4.0.3");
    }
}
