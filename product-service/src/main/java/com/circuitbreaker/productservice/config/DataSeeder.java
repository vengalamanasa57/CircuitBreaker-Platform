package com.circuitbreaker.productservice.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.circuitbreaker.productservice.model.Product;
import com.circuitbreaker.productservice.repository.ProductRepository;

/**
 * Loads a demo catalogue into the in-memory database on startup so the platform
 * is usable immediately. IDs are 1..8 and are referenced by inventory and
 * recommendation seed data.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository repository;

    public DataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Product("Laptop", "14-inch ultrabook, 16 GB RAM, 512 GB SSD", new BigDecimal("1099.00"), "Computers"),
                new Product("Wireless Headphones", "Noise-cancelling over-ear headphones", new BigDecimal("199.99"), "Audio"),
                new Product("Smartphone", "6.1-inch OLED, 128 GB", new BigDecimal("699.00"), "Phones"),
                new Product("Mechanical Keyboard", "Hot-swappable, tactile switches", new BigDecimal("129.50"), "Accessories"),
                new Product("4K Monitor", "27-inch IPS, USB-C power delivery", new BigDecimal("449.00"), "Computers"),
                new Product("Smartwatch", "Heart-rate and GPS tracking", new BigDecimal("249.00"), "Wearables"),
                new Product("Gaming Mouse", "16,000 DPI optical sensor", new BigDecimal("59.99"), "Accessories"),
                new Product("USB-C Hub", "7-in-1 hub with HDMI and card reader", new BigDecimal("39.90"), "Accessories")));
    }
}
