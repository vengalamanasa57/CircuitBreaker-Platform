package com.circuitbreaker.recommendationservice;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.circuitbreaker.recommendationservice.chaos.ChaosMode;
import com.circuitbreaker.recommendationservice.chaos.ChaosState;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "management.tracing.enabled=false"
})
@AutoConfigureMockMvc
class RecommendationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChaosState chaosState;

    @AfterEach
    void healService() {
        chaosState.update(ChaosMode.NONE, null);
    }

    @Test
    void returnsLiveTopSellers() throws Exception {
        mockMvc.perform(get("/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source", is("recommendation-service")))
                .andExpect(jsonPath("$.degraded", is(false)))
                .andExpect(jsonPath("$.recommendations", hasSize(4)))
                .andExpect(jsonPath("$.recommendations[0].name", is("Laptop")));
    }

    @Test
    void returnsRelatedProductsForAProduct() throws Exception {
        mockMvc.perform(get("/recommendations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forProductId", is(1)))
                .andExpect(jsonPath("$.recommendations", hasSize(3)));
    }

    @Test
    void errorChaosModeMakesTheServiceFail() throws Exception {
        mockMvc.perform(post("/internal/chaos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"ERROR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode", is("ERROR")));

        mockMvc.perform(get("/recommendations")).andExpect(status().isInternalServerError());
    }

    @Test
    void resettingChaosHealsTheService() throws Exception {
        chaosState.update(ChaosMode.ERROR, null);
        mockMvc.perform(post("/internal/chaos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"NONE\"}")).andExpect(status().isOk());

        mockMvc.perform(get("/recommendations")).andExpect(status().isOk());
    }

    @Test
    void latencyChaosModeDelaysResponses() throws Exception {
        chaosState.update(ChaosMode.LATENCY, 200);

        long start = System.nanoTime();
        mockMvc.perform(get("/recommendations")).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        org.junit.jupiter.api.Assertions.assertTrue(elapsedMs >= 190, "expected >= 190ms but was " + elapsedMs);
    }

    @Test
    void rejectsInvalidChaosRequest() throws Exception {
        mockMvc.perform(post("/internal/chaos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"NONE\",\"latencyMs\":999999}"))
                .andExpect(status().isBadRequest());
    }
}
