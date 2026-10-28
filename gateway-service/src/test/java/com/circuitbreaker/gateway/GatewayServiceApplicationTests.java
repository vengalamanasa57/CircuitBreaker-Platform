package com.circuitbreaker.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Boots the real gateway context (Eureka off, tracing off) and verifies the
 * routing / resilience wiring declared in application.yml.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "management.tracing.enabled=false"
        })
class GatewayServiceApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void allRoutesAreRegistered() {
        List<String> ids = routeLocator.getRoutes().map(Route::getId).collectList().block();

        assertThat(ids).containsExactlyInAnyOrder(
                "product-service",
                "inventory-service",
                "recommendation-service",
                "recommendation-chaos-admin");
    }

    @Test
    void rootEndpointDescribesTheGatewayInsteadOf404() {
        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("api-gateway");
    }

    @Test
    void recommendationFallbackEndpointServesTopSellers() {
        webTestClient.get().uri("/fallback/recommendation")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Fallback", "true")
                .expectBody()
                .jsonPath("$.degraded").isEqualTo(true)
                .jsonPath("$.recommendations.length()").isEqualTo(4);
    }

    @Test
    void fallbackAcceptsNonGetMethods() {
        webTestClient.post().uri("/fallback/inventory")
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void statusEndpointExposesAllThreeCircuitBreakers() {
        webTestClient.get().uri("/resilience/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.circuitBreakers.length()").isEqualTo(3)
                .jsonPath("$.bulkheads.length()").isEqualTo(2);
    }
}
