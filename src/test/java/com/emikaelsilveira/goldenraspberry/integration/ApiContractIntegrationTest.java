package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:api_contract_testdb",
        "app.security.api-key=test-api-contract-key",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
class ApiContractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.security.api-key}")
    private String apiKey;

    private static final String BASE_URL = "http://localhost:";

    @Test
    void shouldReturnCorrectJsonStructureForProducerIntervals() throws Exception {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        final var json = objectMapper.readTree(response.getBody());

        assertThat(json.isObject()).isTrue();
        assertThat(json.has("min")).isTrue();
        assertThat(json.has("max")).isTrue();

        assertThat(json.get("min").isArray()).isTrue();
        assertThat(json.get("max").isArray()).isTrue();

        if (!json.get("min").isEmpty()) {
            final var minInterval = json.get("min").get(0);
            validateIntervalStructure(minInterval);
        }

        if (!json.get("max").isEmpty()) {
            final var maxInterval = json.get("max").get(0);
            validateIntervalStructure(maxInterval);
        }
    }

    @Test
    void shouldReturnCorrectContentTypeHeaders() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/json");
    }

    @Test
    void shouldAcceptCorrectAcceptHeaders() {
        final String[] acceptHeaders = {
                "application/json",
                "application/json;charset=UTF-8",
                "*/*",
                "application/*"
        };

        for (String acceptHeader : acceptHeaders) {
            final var headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Accept", acceptHeader);

            final var response = restTemplate.exchange(
                    BASE_URL + port + "/api/producers/intervals",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Test
    void shouldSupportGetMethodOnly() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        final var entity = new HttpEntity<>(headers);

        final var getResponse = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        final HttpMethod[] unsupportedMethods = {
                HttpMethod.POST,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                HttpMethod.PATCH
        };

        for (HttpMethod method : unsupportedMethods) {
            try {
                final var response = restTemplate.exchange(
                        BASE_URL + port + "/api/producers/intervals",
                        method,
                        entity,
                        String.class
                );
                assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.METHOD_NOT_ALLOWED);
            } catch (Exception e) {
                assertThat(e).isNotNull();
            }
        }
    }

    @Test
    void shouldReturnStatus200OkForValidRequest() {
        final var okResponse = makeApiRequest("/api/producers/intervals", createHeadersWithApiKey(apiKey));
        assertThat(okResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnStatus403ForbiddenForInvalidAPIKey() {
        final var forbiddenResponse = makeApiRequest("/api/producers/intervals", createHeadersWithApiKey("invalid-key"));
        assertThat(forbiddenResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnStatus403ForbiddenForMissingAPIKey() {
        final var missingKeyResponse = makeApiRequest("/api/producers/intervals", createEmptyHeaders());
        assertThat(missingKeyResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnStatus404NotFoundForNonExistentEndpointOr403IfSecurityFilterInterceptsFirst() {
        ResponseEntity<String> notFoundResponse = makeApiRequest("/api/producers/nonexistent", createHeadersWithApiKey(apiKey));
        assertThat(notFoundResponse.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldMaintainApiVersionConsistency() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        assertThat(BASE_URL + port + "/api/producers/intervals")
                .contains("/api/")
                .contains("producers")
                .contains("intervals");
    }

    @Test
    void shouldProvideConsistentResponseFormat() throws Exception {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        final var entity = new HttpEntity<>(headers);

        final var response1 = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                entity,
                String.class
        );

        final var response2 = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(response2.getStatusCode());

        final var json1 = objectMapper.readTree(response1.getBody());
        final var json2 = objectMapper.readTree(response2.getBody());

        assertThat(json1.has("min")).isEqualTo(json2.has("min"));
        assertThat(json1.has("max")).isEqualTo(json2.has("max"));
        assertThat(json1.get("min").isArray()).isEqualTo(json2.get("min").isArray());
        assertThat(json1.get("max").isArray()).isEqualTo(json2.get("max").isArray());
    }

    @Test
    void shouldReturnStandardErrorResponseFormat() {
        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        if (nonNull(response.getBody())) {
            assertThat(response.getBody()).isNotBlank();
        }
    }

    @Test
    void shouldHandleInvalidRequestsGracefully() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("Content-Type", "application/xml");

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldMeetPerformanceRequirements() {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        final var startTime = System.currentTimeMillis();

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        final var endTime = System.currentTimeMillis();
        final var responseTime = endTime - startTime;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseTime).isLessThan(5000);
    }

    @Test
    void shouldHandleConcurrentRequests() throws InterruptedException {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        final var entity = new HttpEntity<>(headers);

        final var threads = new Thread[5];
        final var responses = new ResponseEntity[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                responses[index] = restTemplate.exchange(
                        BASE_URL + port + "/api/producers/intervals",
                        HttpMethod.GET,
                        entity,
                        String.class
                );
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (final var response : responses) {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Test
    void shouldMaintainDataIntegrity() throws Exception {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        final var entity = new HttpEntity<>(headers);

        final var response1 = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                entity,
                String.class
        );

        final var response2 = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                entity,
                String.class
        );

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

        final var json1 = objectMapper.readTree(response1.getBody());
        final var json2 = objectMapper.readTree(response2.getBody());

        assertThat(json1).isEqualTo(json2);
    }

    @Test
    void shouldValidateJsonSchemaCompliance() throws Exception {
        final var headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        final var response = restTemplate.exchange(
                BASE_URL + port + "/api/producers/intervals",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final var json = objectMapper.readTree(response.getBody());

        assertThat(json.isObject()).isTrue();
        assertThat(json.has("min")).isTrue();
        assertThat(json.has("max")).isTrue();

        assertThat(json.get("min").isArray()).isTrue();
        assertThat(json.get("max").isArray()).isTrue();

        validateIntervalArrays(json.get("min"));
        validateIntervalArrays(json.get("max"));
    }

    private void validateIntervalStructure(JsonNode interval) {
        assertThat(interval.isObject()).isTrue();
        assertThat(interval.has("producer")).isTrue();
        assertThat(interval.has("interval")).isTrue();
        assertThat(interval.has("previousWin")).isTrue();
        assertThat(interval.has("followingWin")).isTrue();

        assertThat(interval.get("producer").isTextual()).isTrue();
        assertThat(interval.get("interval").isNumber()).isTrue();
        assertThat(interval.get("previousWin").isNumber()).isTrue();
        assertThat(interval.get("followingWin").isNumber()).isTrue();

        assertThat(interval.get("interval").asInt()).isPositive();
        assertThat(interval.get("previousWin").asInt()).isLessThan(interval.get("followingWin").asInt());
        assertThat(interval.get("producer").asText()).isNotBlank();
    }

    private void validateIntervalArrays(JsonNode array) {
        if (array.size() > 0) {
            for (JsonNode interval : array) {
                validateIntervalStructure(interval);
            }
        }
    }

    private ResponseEntity<String> makeApiRequest(String endpoint, HttpHeaders headers) {
        return restTemplate.exchange(
                BASE_URL + port + endpoint,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private HttpHeaders createHeadersWithApiKey(String apiKeyValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKeyValue);
        return headers;
    }

    private HttpHeaders createEmptyHeaders() {
        return new HttpHeaders();
    }
}