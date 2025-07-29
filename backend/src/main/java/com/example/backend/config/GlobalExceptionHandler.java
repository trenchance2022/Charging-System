package com.example.backend.config;

import com.example.backend.exception.BatteryCapacityValidationException;
import com.example.backend.exception.ChargingAmountValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理电池容量验证异常
     */
    @ExceptionHandler(BatteryCapacityValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBatteryCapacityValidationException(BatteryCapacityValidationException e) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理充电量验证异常
     */
    @ExceptionHandler(ChargingAmountValidationException.class)
    public ResponseEntity<Map<String, String>> handleChargingAmountValidationException(ChargingAmountValidationException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理ResponseStatusException
     * 这个异常应该保持其原始状态码，不应该被转换为400
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getReason() != null ? e.getReason() : "请求处理失败");
        return new ResponseEntity<>(response, e.getStatusCode());
    }

    /**
     * 处理运行时异常
     * 注意：这个处理器不会处理ResponseStatusException，因为上面的处理器更加具体
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "服务器内部错误");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 