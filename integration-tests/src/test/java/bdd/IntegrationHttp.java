package bdd;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

public class IntegrationHttp {

    private final RestClient client = RestClient.builder().build();
    private final IntegrationWorld world;

    public IntegrationHttp(IntegrationWorld world) {
        this.world = world;
    }

    public void post(String baseUrl, String path, String json, String token) {
        var spec = client.post()
                .uri(baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        spec.exchange((request, response) -> {
            world.setStatus(response.getStatusCode().value());
            world.setBody(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            return null;
        });
    }

    public void get(String baseUrl, String path, String token) {
        var spec = client.get().uri(baseUrl + path);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        spec.exchange((request, response) -> {
            world.setStatus(response.getStatusCode().value());
            world.setBody(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            return null;
        });
    }

    public void delete(String baseUrl, String path, String token) {
        var spec = client.delete().uri(baseUrl + path);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        spec.exchange((request, response) -> {
            world.setStatus(response.getStatusCode().value());
            world.setBody(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8));
            return null;
        });
    }
}
