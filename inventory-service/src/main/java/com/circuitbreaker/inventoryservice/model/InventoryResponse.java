package com.circuitbreaker.inventoryservice.model;

public record InventoryResponse(Long productId, int quantity, boolean inStock) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(inventory.getProductId(), inventory.getQuantity(), inventory.getQuantity() > 0);
    }
}
