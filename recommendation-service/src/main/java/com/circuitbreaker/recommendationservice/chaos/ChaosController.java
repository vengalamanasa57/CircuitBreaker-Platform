package com.circuitbreaker.recommendationservice.chaos;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * Fault-injection control plane. Lives under {@code /internal} and is reached via
 * the gateway's dedicated admin route ({@code /admin/recommendation/chaos}), which
 * deliberately bypasses the circuit breaker so the service can be healed while
 * the breaker is OPEN.
 *
 * <p>DEMO ONLY - never expose this in production.
 */
@RestController
@RequestMapping("/internal/chaos")
public class ChaosController {

    private final ChaosState chaosState;

    public ChaosController(ChaosState chaosState) {
        this.chaosState = chaosState;
    }

    @GetMapping
    public ChaosState.Snapshot current() {
        return chaosState.get();
    }

    @PostMapping
    public ChaosState.Snapshot update(@Valid @RequestBody ChaosRequest request) {
        return chaosState.update(request.mode(), request.latencyMs());
    }
}
