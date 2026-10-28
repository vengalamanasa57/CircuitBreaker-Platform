package com.circuitbreaker.productservice.model;

import java.math.BigDecimal;

/** Outbound representation - the JPA entity is never exposed directly. */
public record ProductResponse(Long id, String name, String description, BigDecimal price, String category) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategory());
    }
}
