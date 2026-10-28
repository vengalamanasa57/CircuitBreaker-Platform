package com.circuitbreaker.productservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.circuitbreaker.productservice.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryIgnoreCase(String category);
}
