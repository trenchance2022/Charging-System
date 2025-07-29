package com.example.backend.exception;

/**
 * 充电量验证异常
 */
public class ChargingAmountValidationException extends RuntimeException {
    
    public ChargingAmountValidationException(String message) {
        super(message);
    }
    
    public ChargingAmountValidationException(String message, Throwable cause) {
        super(message, cause);
    }
} 