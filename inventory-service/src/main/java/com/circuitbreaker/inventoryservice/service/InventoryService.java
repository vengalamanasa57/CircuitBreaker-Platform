package com.circuitbreaker.inventoryservice.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.circuitbreaker.inventoryservice.exception.InsufficientStockException;
import com.circuitbreaker.inventoryservice.exception.InventoryNotFoundException;
import com.circuitbreaker.inventoryservice.model.Inventory;
import com.circuitbreaker.inventoryservice.repository.InventoryRepository;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    public List<Inventory> findAll() {
        return repository.findAll();
    }

    public Inventory findByProductId(Long productId) {
        return repository.findById(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    @Transactional
    public Inventory create(Long productId, int quantity) {
        if (repository.existsById(productId)) {
            throw new DataIntegrityViolationException("Inventory already exists for product " + productId);
        }
        return repository.save(new Inventory(productId, quantity));
    }

    @Transactional
    public Inventory setQuantity(Long productId, int quantity) {
        Inventory inventory = findByProductId(productId);
        inventory.setQuantity(quantity);
        return inventory;
    }

    /** Atomically removes {@code units} from stock; rejects the request when stock is insufficient. */
    @Transactional
    public Inventory reserve(Long productId, int units) {
        Inventory inventory = findByProductId(productId);
        if (inventory.getQuantity() < units) {
            throw new InsufficientStockException(productId, units, inventory.getQuantity());
        }
        inventory.setQuantity(inventory.getQuantity() - units);
        return inventory;
    }

    @Transactional
    public void delete(Long productId) {
        if (!repository.existsById(productId)) {
            throw new InventoryNotFoundException(productId);
        }
        repository.deleteById(productId);
    }
}
