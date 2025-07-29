package com.example.backend.model.dto;

/**
 * 登录响应DTO
 */
public class LoginResponse {
    
    private String token;
    private String type;
    
    public LoginResponse(String token, String type) {
        this.token = token;
        this.type = type;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
} 