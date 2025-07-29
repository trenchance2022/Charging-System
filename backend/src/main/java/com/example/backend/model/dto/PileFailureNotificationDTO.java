package com.example.backend.model.dto;

import java.time.LocalDateTime;

/**
 * 充电桩故障通知DTO
 * 用于向用户推送充电桩故障信息
 * 
 * @author System
 * @since 1.0
 */
public class PileFailureNotificationDTO {
    
    /**
     * 通知类型
     */
    private String notificationType;
    
    /**
     * 故障充电桩编号
     */
    private String pileNumber;
    
    /**
     * 充电请求ID
     */
    private String requestId;
    
    /**
     * 通知标题
     */
    private String title;
    
    /**
     * 通知消息内容
     */
    private String message;
    
    /**
     * 通知时间
     */
    private LocalDateTime notificationTime;
    
    /**
     * 通知级别（INFO, WARNING, ERROR）
     */
    private String level;
    
    public PileFailureNotificationDTO() {
        this.notificationTime = LocalDateTime.now();
    }
    
    public PileFailureNotificationDTO(String notificationType, String pileNumber, String requestId, 
                                    String title, String message, String level) {
        this.notificationType = notificationType;
        this.pileNumber = pileNumber;
        this.requestId = requestId;
        this.title = title;
        this.message = message;
        this.level = level;
        this.notificationTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getNotificationType() {
        return notificationType;
    }
    
    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }
    
    public String getPileNumber() {
        return pileNumber;
    }
    
    public void setPileNumber(String pileNumber) {
        this.pileNumber = pileNumber;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getNotificationTime() {
        return notificationTime;
    }
    
    public void setNotificationTime(LocalDateTime notificationTime) {
        this.notificationTime = notificationTime;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
} 