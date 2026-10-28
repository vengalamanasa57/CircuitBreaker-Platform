package com.circuitbreaker.recommendationservice.chaos;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Holds the current fault-injection setting and applies it to incoming requests.
 * This is what lets the dashboard "break" the service on demand and prove that
 * the gateway's circuit breaker protects the rest of the platform.
 */
@Component
public class ChaosState {

    public record Snapshot(ChaosMode mode, int latencyMs) {
    }

    private static final int DEFAULT_LATENCY_MS = 4_000;   // > gateway TimeLimiter (3s)

    private final AtomicReference<Snapshot> current =
            new AtomicReference<>(new Snapshot(ChaosMode.NONE, DEFAULT_LATENCY_MS));

    public Snapshot get() {
        return current.get();
    }

    public Snapshot update(ChaosMode mode, Integer latencyMs) {
        int latency = latencyMs != null ? latencyMs : current.get().latencyMs();
        Snapshot next = new Snapshot(mode, latency);
        current.set(next);
        return next;
    }

    /** Called at the start of every business request. */
    public void applyTo() {
        Snapshot snapshot = current.get();
        switch (snapshot.mode()) {
            case LATENCY -> sleep(snapshot.latencyMs());
            case ERROR -> throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Chaos mode: injected failure");
            case NONE -> {
                // healthy path
            }
        }
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
