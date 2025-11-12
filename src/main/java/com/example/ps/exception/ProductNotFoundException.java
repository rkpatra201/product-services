package com.example.ps.exception;

public class ProductNotFoundException extends RuntimeException {
    
    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
    }
    
    public ProductNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
