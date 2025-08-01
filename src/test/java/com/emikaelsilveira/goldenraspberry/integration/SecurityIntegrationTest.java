package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:security_testdb",
        "app.security.api-key=test-api-key-123",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.security.api-key}")
    private String validApiKey;

    @Test
    void shouldAllowAccessWithValidApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldDenyAccessWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", "invalid-api-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithoutApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithEmptyApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDenyAccessWithNullApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldBeCaseSensitiveForApiKey() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", validApiKey.toUpperCase())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectWhenHeaderNameApiKeyMissingXPrefix() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("api-key", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectWhenHeaderNameAuthorizationIsWrongHeaderType() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("Authorization", "Bearer " + validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDifferentHeaderNames() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-auth-token", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowActuatorEndpointsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyActuatorEndpointsWithInvalidApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("x-api-key", "invalid-key"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleMultipleApiKeyHeadersAndShouldUseFirstHeaderValue() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", validApiKey)
                        .header("x-api-key", "another-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectApiKeyWithWhitespace() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", " " + validApiKey + " ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldNotExposeInternalSecurityDetails() throws Exception {
        mockMvc.perform(get("/api/producers/intervals"))
                .andExpect(status().isForbidden())
                .andExpect(result -> {
                    final var responseBody = result.getResponse().getContentAsString();
                    assert !responseBody.toLowerCase().contains("apikey");
                    assert !responseBody.toLowerCase().contains("filter");
                    assert !responseBody.toLowerCase().contains("security");
                });
    }
}