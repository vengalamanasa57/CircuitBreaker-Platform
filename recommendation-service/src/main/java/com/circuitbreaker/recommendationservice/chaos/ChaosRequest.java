package com.circuitbreaker.recommendationservice.chaos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChaosRequest(
        @NotNull(message = "mode is required") ChaosMode mode,
        @Min(0) @Max(30_000) Integer latencyMs) {
}
