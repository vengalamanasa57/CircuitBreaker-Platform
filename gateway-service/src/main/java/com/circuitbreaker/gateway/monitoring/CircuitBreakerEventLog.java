package com.circuitbreaker.gateway.monitoring;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Keeps a bounded, in-memory timeline of circuit breaker state transitions
 * (CLOSED -> OPEN -> HALF_OPEN -> CLOSED ...) so the dashboard can show
 * <em>when</em> a breaker tripped and recovered, not just its current state.
 */
@Component
public class CircuitBreakerEventLog {

    private static final int MAX_EVENTS = 50;

    public record TransitionEvent(Instant time, String breaker, String from, String to) {
    }

    private final Deque<TransitionEvent> events = new ArrayDeque<>();

    public CircuitBreakerEventLog(CircuitBreakerRegistry registry) {
        registry.getAllCircuitBreakers().forEach(this::listenTo);
        registry.getEventPublisher().onEntryAdded(event -> listenTo(event.getAddedEntry()));
    }

    private void listenTo(CircuitBreaker breaker) {
        breaker.getEventPublisher().onStateTransition(event -> record(new TransitionEvent(
                event.getCreationTime().toInstant(),
                event.getCircuitBreakerName(),
                event.getStateTransition().getFromState().name(),
                event.getStateTransition().getToState().name())));
    }

    private synchronized void record(TransitionEvent event) {
        events.addFirst(event);
        while (events.size() > MAX_EVENTS) {
            events.removeLast();
        }
    }

    /** Newest first. */
    public synchronized List<TransitionEvent> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }
}
