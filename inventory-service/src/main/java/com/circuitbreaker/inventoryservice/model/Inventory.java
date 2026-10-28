package com.circuitbreaker.inventoryservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/** Stock level for one product. The primary key is the product id owned by product-service. */
@Entity
public class Inventory {

    @Id
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    /** Optimistic locking: two concurrent reservations cannot both succeed on stale data. */
    @Version
    private Long version;

    protected Inventory() {
        // required by JPA
    }

    public Inventory(Long productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getVersion() {
        return version;
    }
}
