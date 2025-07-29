package com.example.backend.model.dto;

/**
 * 注册响应数据传输对象
 */
public class RegisterResponse {
    private String message;

    public RegisterResponse() {
    }

    public RegisterResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 