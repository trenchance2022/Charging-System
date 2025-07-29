package com.example.backend.exception;

/**
 * 电池容量验证异常
 */
public class BatteryCapacityValidationException extends RuntimeException {
    
    public BatteryCapacityValidationException(String message) {
        super(message);
    }
    
    public BatteryCapacityValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 