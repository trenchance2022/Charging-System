package com.example.backend.model.dto;

public class QueueStatusDTO {
    private String queueNumber;    // 固定的排队号码，不随队列变化
    private int queueCount;     // 前方等待的车辆数，随队列变化
    private int estimatedWait;  // 预计等待时间（分钟）
    private String locationType; // WAITING_AREA 表示在等候区, CHARGING_PILE 表示在充电桩队列
    
    public QueueStatusDTO() {
    }
    
    public QueueStatusDTO(String queueNumber, int queueCount, int estimatedWait) {
        this.queueNumber = queueNumber;
        this.queueCount = queueCount;
        this.estimatedWait = estimatedWait;
        this.locationType = "NONE"; // 默认值
    }
    
    public QueueStatusDTO(String queueNumber, int queueCount, int estimatedWait, String locationType) {
        this.queueNumber = queueNumber;
        this.queueCount = queueCount;
        this.estimatedWait = estimatedWait;
        this.locationType = locationType;
    }
    
    public String getQueueNumber() {
        return queueNumber;
    }
    
    public void setQueueNumber(String queueNumber) {
        this.queueNumber = queueNumber;
    }
    
    public int getQueueCount() {
        return queueCount;
    }
    
    public void setQueueCount(int queueCount) {
        this.queueCount = queueCount;
    }
    
    public int getEstimatedWait() {
        return estimatedWait;
    }
    
    public void setEstimatedWait(int estimatedWait) {
        this.estimatedWait = estimatedWait;
    }
    
    public String getLocationType() {
        return locationType;
    }
    
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
} 