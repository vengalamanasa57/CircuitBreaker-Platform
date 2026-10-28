package com.circuitbreaker.inventoryservice.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Cannot reserve " + requested + " unit(s) of product " + productId
                + ": only " + available + " in stock");
    }
}
