package com.circuitbreaker.gateway.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints the gateway forwards to when a circuit breaker is OPEN, a call times
 * out, or the downstream service answers with a 5xx.
 *
 * <p>Mappings accept every HTTP method because a forwarded request keeps its
 * original method (a failed POST /inventory must not turn into a 405).
 *
 * <ul>
 *   <li><b>Recommendations</b> are non-critical, so we degrade gracefully with a
 *       200 and a static "Top Sellers" list.</li>
 *   <li><b>Products / Inventory</b> are authoritative data; inventing a response
 *       would be wrong, so we return an honest 503.</li>
 * </ul>
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    static final String FALLBACK_HEADER = "X-Fallback";

    private static final List<Map<String, Object>> TOP_SELLERS = List.of(
            item(1L, "Laptop"),
            item(2L, "Wireless Headphones"),
            item(3L, "Smartphone"),
            item(4L, "Mechanical Keyboard"));

    @RequestMapping("/recommendation")
    public ResponseEntity<Map<String, Object>> recommendationFallback() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", "gateway-fallback");
        body.put("degraded", true);
        body.put("service", "recommendation-service");
        body.put("status", "fallback");
        body.put("message",
                "Recommendation service is temporarily unavailable. Showing top sellers instead.");
        body.put("recommendations", TOP_SELLERS);
        return ResponseEntity.ok()
                .header(FALLBACK_HEADER, "true")
                .body(body);
    }

    @RequestMapping("/product")
    public ResponseEntity<Map<String, String>> productFallback() {
        return unavailable("product-service");
    }

    @RequestMapping("/inventory")
    public ResponseEntity<Map<String, String>> inventoryFallback() {
        return unavailable("inventory-service");
    }

    private static ResponseEntity<Map<String, String>> unavailable(String service) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", service);
        body.put("status", "unavailable");
        body.put("message", service + " is temporarily unavailable. Please try again later.");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(FALLBACK_HEADER, "true")
                .body(body);
    }

    private static Map<String, Object> item(long productId, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productId", productId);
        m.put("name", name);
        m.put("reason", "Top seller");
        return m;
    }
}
