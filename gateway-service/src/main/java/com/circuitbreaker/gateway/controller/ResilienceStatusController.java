package com.circuitbreaker.gateway.controller;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.circuitbreaker.gateway.monitoring.CircuitBreakerEventLog;
import com.circuitbreaker.gateway.monitoring.ServiceHealthProbe;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import reactor.core.publisher.Mono;

/**
 * Single read-only endpoint feeding the monitoring dashboard: live circuit
 * breaker state + metrics, bulkhead usage, service health and the recent
 * state-transition timeline.
 *
 * <p>A purpose-built endpoint is used instead of {@code /actuator/health}
 * because health aggregation hides per-breaker details and changes shape
 * depending on the state, which makes it a poor UI contract.
 */
@RestController
@RequestMapping("/resilience")
public class ResilienceStatusController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final CircuitBreakerEventLog eventLog;
    private final ServiceHealthProbe healthProbe;

    public ResilienceStatusController(CircuitBreakerRegistry circuitBreakerRegistry,
                                      BulkheadRegistry bulkheadRegistry,
                                      CircuitBreakerEventLog eventLog,
                                      ServiceHealthProbe healthProbe) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
        this.eventLog = eventLog;
        this.healthProbe = healthProbe;
    }

    @GetMapping("/status")
    public Mono<Map<String, Object>> status() {
        return healthProbe.probeAll().map(services -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("timestamp", Instant.now().toString());
            body.put("gateway", "UP");
            body.put("services", services);
            body.put("circuitBreakers", circuitBreakers());
            body.put("bulkheads", bulkheads());
            body.put("events", eventLog.snapshot());
            return body;
        });
    }

    private List<Map<String, Object>> circuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .sorted(Comparator.comparing(CircuitBreaker::getName))
                .map(ResilienceStatusController::describe)
                .toList();
    }

    private static Map<String, Object> describe(CircuitBreaker breaker) {
        CircuitBreaker.Metrics metrics = breaker.getMetrics();
        var config = breaker.getCircuitBreakerConfig();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", breaker.getName());
        m.put("state", breaker.getState().name());
        // Resilience4j returns -1 until minimumNumberOfCalls have been recorded.
        m.put("failureRate", metrics.getFailureRate() < 0 ? null : round(metrics.getFailureRate()));
        m.put("slowCallRate", metrics.getSlowCallRate() < 0 ? null : round(metrics.getSlowCallRate()));
        m.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        m.put("successfulCalls", metrics.getNumberOfSuccessfulCalls());
        m.put("failedCalls", metrics.getNumberOfFailedCalls());
        m.put("notPermittedCalls", metrics.getNumberOfNotPermittedCalls());
        m.put("failureRateThreshold", config.getFailureRateThreshold());
        m.put("slidingWindowSize", config.getSlidingWindowSize());
        m.put("minimumNumberOfCalls", config.getMinimumNumberOfCalls());
        m.put("waitDurationInOpenStateMs", config.getWaitIntervalFunctionInOpenState().apply(1));
        return m;
    }

    private List<Map<String, Object>> bulkheads() {
        // Spring Cloud CircuitBreaker also registers an internal bulkhead per breaker name; hide those.
        Set<String> breakerNames = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .map(CircuitBreaker::getName)
                .collect(Collectors.toSet());
        return bulkheadRegistry.getAllBulkheads().stream()
                .filter(b -> !breakerNames.contains(b.getName()))
                .sorted(Comparator.comparing(Bulkhead::getName))
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", b.getName());
                    m.put("maxConcurrentCalls", b.getBulkheadConfig().getMaxConcurrentCalls());
                    m.put("availableConcurrentCalls", b.getMetrics().getAvailableConcurrentCalls());
                    return m;
                })
                .toList();
    }

    private static double round(float value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
