package com.circuitbreaker.inventoryservice.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.circuitbreaker.inventoryservice.model.Inventory;
import com.circuitbreaker.inventoryservice.model.InventoryRequest;
import com.circuitbreaker.inventoryservice.model.InventoryResponse;
import com.circuitbreaker.inventoryservice.model.StockUpdateRequest;
import com.circuitbreaker.inventoryservice.service.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryResponse> list() {
        return service.findAll().stream().map(InventoryResponse::from).toList();
    }

    @GetMapping("/{productId}")
    public InventoryResponse get(@PathVariable Long productId) {
        return InventoryResponse.from(service.findByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        Inventory created = service.create(request.productId(), request.quantity());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}").buildAndExpand(created.getProductId()).toUri();
        return ResponseEntity.created(location).body(InventoryResponse.from(created));
    }

    @PutMapping("/{productId}")
    public InventoryResponse setQuantity(@PathVariable Long productId,
                                         @Valid @RequestBody StockUpdateRequest request) {
        return InventoryResponse.from(service.setQuantity(productId, request.quantity()));
    }

    /** Checkout-style operation: take {@code quantity} units out of stock or fail with 409. */
    @PostMapping("/{productId}/reserve")
    public InventoryResponse reserve(@PathVariable Long productId,
                                     @RequestParam(defaultValue = "1") @Min(1) int quantity) {
        return InventoryResponse.from(service.reserve(productId, quantity));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(@PathVariable Long productId) {
        service.delete(productId);
        return ResponseEntity.noContent().build();
    }
}
