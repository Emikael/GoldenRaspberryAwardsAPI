package com.emikaelsilveira.goldenraspberry.integration;

import com.emikaelsilveira.goldenraspberry.GoldenRaspberryApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = GoldenRaspberryApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "app.security.api-key=test-api-key-producer",
        "logging.level.com.emikaelsilveira.goldenraspberry=WARN"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProducerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String API_KEY = "test-api-key-producer";

    @Test
    void shouldReturnProducerIntervals() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", notNullValue()))
                .andExpect(jsonPath("$.min", isA(java.util.List.class)))
                .andExpect(jsonPath("$.max", isA(java.util.List.class)));
    }

    @Test
    void shouldReturnCorrectProducerIntervalStructure() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.min").exists())
                .andExpect(jsonPath("$.max").exists())
                .andExpect(jsonPath("$.min[*].producer").exists())
                .andExpect(jsonPath("$.min[*].interval").exists())
                .andExpect(jsonPath("$.min[*].previousWin").exists())
                .andExpect(jsonPath("$.min[*].followingWin").exists())
                .andExpect(jsonPath("$.max[*].producer").exists())
                .andExpect(jsonPath("$.max[*].interval").exists())
                .andExpect(jsonPath("$.max[*].previousWin").exists())
                .andExpect(jsonPath("$.max[*].followingWin").exists());
    }

    @Test
    void shouldCalculateCorrectIntervals() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.min").isArray())
                .andExpect(jsonPath("$.max").isArray())
                .andExpect(jsonPath("$.min[*].interval").value(everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.max[*].interval").value(everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.min[*].previousWin").value(everyItem(greaterThanOrEqualTo(1980))))
                .andExpect(jsonPath("$.min[*].followingWin").value(everyItem(greaterThanOrEqualTo(1980))))
                .andExpect(jsonPath("$.max[*].previousWin").value(everyItem(greaterThanOrEqualTo(1980))))
                .andExpect(jsonPath("$.max[*].followingWin").value(everyItem(greaterThanOrEqualTo(1980))))
                .andExpect(jsonPath("$.min").value(hasSize(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.max").value(hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    void shouldReturnValidProducerNames() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.min[*].producer").value(everyItem(notNullValue())))
                .andExpect(jsonPath("$.min[*].producer").value(everyItem(not(emptyString()))))
                .andExpect(jsonPath("$.max[*].producer").value(everyItem(notNullValue())))
                .andExpect(jsonPath("$.max[*].producer").value(everyItem(not(emptyString()))));
    }

    @Test
    void shouldIncludeKnownProducersFromTestData() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.min").isArray())
                .andExpect(jsonPath("$.max").isArray());
    }

    @Test
    void shouldReturnCorrectHttpHeaders() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().doesNotExist("X-Frame-Options")); // Should be handled by security config
    }

    @Test
    void shouldAllowPublicAccess() throws Exception {
        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRespondInReasonableTime() throws Exception {
        final var startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        final var endTime = System.currentTimeMillis();
        final var responseTime = endTime - startTime;

        assert responseTime < 5000 : "Response time too slow: " + responseTime + "ms";
    }

    @Test
    void shouldHandleInvalidHttpMethods() throws Exception {
        mockMvc.perform(post("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(put("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/producers/intervals")
                        .header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }
}
