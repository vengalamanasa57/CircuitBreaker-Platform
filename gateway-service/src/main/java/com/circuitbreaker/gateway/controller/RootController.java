package com.circuitbreaker.gateway.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Friendly landing response so opening the gateway in a browser is self-explanatory instead of a 404. */
@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("products", "/products");
        endpoints.put("inventory", "/inventory/{productId}");
        endpoints.put("recommendations", "/recommendations");
        endpoints.put("resilienceStatus", "/resilience/status");
        endpoints.put("health", "/actuator/health");
        endpoints.put("metrics", "/actuator/prometheus");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "api-gateway");
        body.put("description", "Resilient API gateway: rate limiting, bulkheads, time limits, retries and circuit breakers");
        body.put("dashboard", "http://localhost:5173 (local) or http://localhost:3000 (Docker)");
        body.put("endpoints", endpoints);
        return body;
    }
}
