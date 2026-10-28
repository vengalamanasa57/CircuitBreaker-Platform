package com.circuitbreaker.inventoryservice.model;

import jakarta.validation.constraints.Min;

public record StockUpdateRequest(@Min(value = 0, message = "quantity must be >= 0") int quantity) {
}
