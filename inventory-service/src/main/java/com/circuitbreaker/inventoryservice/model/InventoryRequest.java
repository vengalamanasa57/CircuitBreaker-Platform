package com.circuitbreaker.inventoryservice.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
        @NotNull(message = "productId is required") Long productId,
        @Min(value = 0, message = "quantity must be >= 0") int quantity) {
}
