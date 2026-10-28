package com.circuitbreaker.productservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.circuitbreaker.productservice.exception.ProductNotFoundException;
import com.circuitbreaker.productservice.model.Product;
import com.circuitbreaker.productservice.model.ProductRequest;
import com.circuitbreaker.productservice.repository.ProductRepository;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll(String category) {
        if (category == null || category.isBlank()) {
            return repository.findAll();
        }
        return repository.findByCategoryIgnoreCase(category.trim());
    }

    public Product findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        return repository.save(new Product(
                request.name(), request.description(), request.price(), request.category()));
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(request.category());
        return product; // managed entity - flushed on commit
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
