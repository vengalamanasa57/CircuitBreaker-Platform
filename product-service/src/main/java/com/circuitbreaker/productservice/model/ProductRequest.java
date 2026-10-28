package com.circuitbreaker.productservice.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Inbound payload for create / update. Validated before it reaches the service layer. */
public record ProductRequest(
        @NotBlank(message = "name is required") @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull(message = "price is required") @DecimalMin(value = "0.0", message = "price must be >= 0") BigDecimal price,
        @NotBlank(message = "category is required") @Size(max = 60) String category) {
}
