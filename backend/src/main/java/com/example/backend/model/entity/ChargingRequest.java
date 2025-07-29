package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("charging_request")
public class ChargingRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String requestId;  // 请求ID，格式为F1、F2或T1、T2等
    private Long userId;  // 用户ID
    private String chargingMode;  // 充电模式：fast或slow
    private Double chargingAmount;  // 请求充电量，单位kWh
    private String status;  // 状态：WAITING(等待中)、CHARGING(充电中)、COMPLETED(已完成)、CANCELED(已取消)
    private LocalDateTime createTime;  // 创建时间
    private LocalDateTime startTime;  // 开始充电时间
    private LocalDateTime endTime;  // 结束充电时间
    private Integer chargingPileId;  // 分配的充电桩ID
    private Integer queuePosition;  // 在充电桩队列中的位置

    public ChargingRequest() {}

    public ChargingRequest(String requestId, Long userId, String chargingMode, Double chargingAmount, String status, LocalDateTime createTime, LocalDateTime startTime, LocalDateTime endTime, Integer chargingPileId, Integer queuePosition) {
        this.requestId = requestId;
        this.userId = userId;
        this.chargingMode = chargingMode;
        this.chargingAmount = chargingAmount;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getChargingMode() {
        return chargingMode;
    }
    
    public void setChargingMode(String chargingMode) {
        this.chargingMode = chargingMode;
    }

    public Double getChargingAmount() {
        return chargingAmount;
    }
    
    public void setChargingAmount(Double chargingAmount) {
        this.chargingAmount = chargingAmount;
    }

    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getChargingPileId() {
        return chargingPileId;
    }
    
    public void setChargingPileId(Integer chargingPileId) {
        this.chargingPileId = chargingPileId;
    }

    public Integer getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(Integer queuePosition) {
        this.queuePosition = queuePosition;
    }
} 