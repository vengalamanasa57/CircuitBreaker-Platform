package com.circuitbreaker.inventoryservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circuitbreaker.inventoryservice.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
