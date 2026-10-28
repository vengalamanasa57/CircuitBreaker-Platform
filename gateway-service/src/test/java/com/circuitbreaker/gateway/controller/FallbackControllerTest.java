package com.circuitbreaker.gateway.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void recommendationFallbackDegradesGracefullyWithTopSellers() {
        ResponseEntity<Map<String, Object>> response = controller.recommendationFallback();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("true", response.getHeaders().getFirst(FallbackController.FALLBACK_HEADER));

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("gateway-fallback", body.get("source"));
        assertEquals(Boolean.TRUE, body.get("degraded"));
        assertEquals("recommendation-service", body.get("service"));
        assertTrue(body.get("message").toString().toLowerCase().contains("top sellers"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("recommendations");
        assertEquals(4, items.size());
        assertEquals("Laptop", items.get(0).get("name"));
    }

    @Test
    void productFallbackIsAnHonest503() {
        ResponseEntity<Map<String, String>> response = controller.productFallback();

        assertEquals(503, response.getStatusCode().value());
        assertEquals("product-service", response.getBody().get("service"));
        assertEquals("unavailable", response.getBody().get("status"));
    }

    @Test
    void inventoryFallbackIsAnHonest503() {
        ResponseEntity<Map<String, String>> response = controller.inventoryFallback();

        assertEquals(503, response.getStatusCode().value());
        assertEquals("inventory-service", response.getBody().get("service"));
        assertEquals("unavailable", response.getBody().get("status"));
    }
}
