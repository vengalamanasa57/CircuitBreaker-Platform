package com.circuitbreaker.recommendationservice.chaos;

public enum ChaosMode {
    /** Normal behaviour. */
    NONE,
    /** Every request is delayed by {@code latencyMs} - simulates an overloaded service. */
    LATENCY,
    /** Every request fails with HTTP 500 - simulates a broken release. */
    ERROR
}
