package com.example.backend.model.dto;

public class ChargingResponseDTO {
    private String requestId;  // 请求ID
    private String message;  // 响应消息
    private String status;  // 状态

    public ChargingResponseDTO() {}

    public ChargingResponseDTO(String requestId, String message, String status) {
        this.requestId = requestId;
        this.message = message;
        this.status = status;
    }
    
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 