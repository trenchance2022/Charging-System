package com.example.backend.model.dto;

/**
 * 注册请求数据传输对象
 */
public class RegisterRequest {
    private String username;
    private String password;
    private String type;

    // 构造函数
    public RegisterRequest() {
    }

    public RegisterRequest(String username, String password, String type) {
        this.username = username;
        this.password = password;
        this.type = type;
    }

    // Getter 和 Setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
} 