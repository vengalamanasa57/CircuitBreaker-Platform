package com.circuitbreaker.inventoryservice.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.circuitbreaker.inventoryservice.model.Inventory;
import com.circuitbreaker.inventoryservice.repository.InventoryRepository;

/** Seeds stock for product ids 1..8 (matching product-service's demo catalogue). */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final InventoryRepository repository;

    public DataSeeder(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Inventory(1L, 25),
                new Inventory(2L, 120),
                new Inventory(3L, 60),
                new Inventory(4L, 80),
                new Inventory(5L, 15),
                new Inventory(6L, 45),
                new Inventory(7L, 200),
                new Inventory(8L, 0)));   // intentionally out of stock to demo inStock=false
    }
}
