package com.example.ps.exception;

public class InvalidRecommendationQueryException extends RuntimeException {
    
    public InvalidRecommendationQueryException(String message) {
        super(message);
    }
    
    public InvalidRecommendationQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
