package com.example.backend.model.dto;

import java.math.BigDecimal;

public class ChargingStatusDTO {
    private String status;  // 充电状态
    private Double currentPower;  // 当前电量（电池当前储电量）
    private Double chargedAmount;  // 已充电量（本次充电请求的已充电量）
    private Double totalCapacity;  // 电池总量
    private Double requestedAmount;  // 请求充电量
    private Integer remainingTime;  // 剩余充电时间，单位分钟
    private Boolean isQueueFirst;  // 是否在充电桩队列队首
    private Integer chargingPileId;  // 充电桩ID
    private Boolean isAutoCompleted;  // 是否为自动完成
    private String chargingPileStatus;  // 充电桩状态
    
    // 计费相关字段
    private BigDecimal currentTotalFee;     // 当前已产生的总费用（元）
    private BigDecimal estimatedTotalFee;   // 预计总费用（元）

    public ChargingStatusDTO() {}

    public ChargingStatusDTO(String status, Double currentPower, Double totalCapacity, Integer remainingTime) {
        this.status = status;
        this.currentPower = currentPower;
        this.chargedAmount = 0.0;
        this.totalCapacity = totalCapacity;
        this.requestedAmount = 0.0;
        this.remainingTime = remainingTime;
        this.isQueueFirst = false;
        this.chargingPileId = null;
        this.isAutoCompleted = false;
        // 初始化计费字段
        this.currentTotalFee = BigDecimal.ZERO;
        this.estimatedTotalFee = BigDecimal.ZERO;
    }

    public ChargingStatusDTO(String status, Double currentPower, Double chargedAmount, Double totalCapacity, Double requestedAmount, Integer remainingTime, Boolean isQueueFirst, Integer chargingPileId) {
        this.status = status;
        this.currentPower = currentPower;
        this.chargedAmount = chargedAmount;
        this.totalCapacity = totalCapacity;
        this.requestedAmount = requestedAmount;
        this.remainingTime = remainingTime;
        this.isQueueFirst = isQueueFirst;
        this.chargingPileId = chargingPileId;
        this.isAutoCompleted = false;
        // 初始化计费字段
        this.currentTotalFee = BigDecimal.ZERO;
        this.estimatedTotalFee = BigDecimal.ZERO;
    }

    public ChargingStatusDTO(String status, Double currentPower, Double chargedAmount, Double totalCapacity, Double requestedAmount, Integer remainingTime, Boolean isQueueFirst, Integer chargingPileId, Boolean isAutoCompleted) {
        this.status = status;
        this.currentPower = currentPower;
        this.chargedAmount = chargedAmount;
        this.totalCapacity = totalCapacity;
        this.requestedAmount = requestedAmount;
        this.remainingTime = remainingTime;
        this.isQueueFirst = isQueueFirst;
        this.chargingPileId = chargingPileId;
        this.isAutoCompleted = isAutoCompleted;
        // 初始化计费字段
        this.currentTotalFee = BigDecimal.ZERO;
        this.estimatedTotalFee = BigDecimal.ZERO;
    }

    public ChargingStatusDTO(String status, Double currentPower, Double chargedAmount, Double totalCapacity, Double requestedAmount, Integer remainingTime, Boolean isQueueFirst, Integer chargingPileId, Boolean isAutoCompleted, String chargingPileStatus) {
        this.status = status;
        this.currentPower = currentPower;
        this.chargedAmount = chargedAmount;
        this.totalCapacity = totalCapacity;
        this.requestedAmount = requestedAmount;
        this.remainingTime = remainingTime;
        this.isQueueFirst = isQueueFirst;
        this.chargingPileId = chargingPileId;
        this.isAutoCompleted = isAutoCompleted;
        this.chargingPileStatus = chargingPileStatus;
        // 初始化计费字段
        this.currentTotalFee = BigDecimal.ZERO;
        this.estimatedTotalFee = BigDecimal.ZERO;
    }

    // 完整构造函数，包含计费信息
    public ChargingStatusDTO(String status, Double currentPower, Double chargedAmount, Double totalCapacity, 
                           Double requestedAmount, Integer remainingTime, Boolean isQueueFirst, Integer chargingPileId, 
                           Boolean isAutoCompleted, String chargingPileStatus,
                           BigDecimal currentTotalFee, BigDecimal estimatedTotalFee) {
        this.status = status;
        this.currentPower = currentPower;
        this.chargedAmount = chargedAmount;
        this.totalCapacity = totalCapacity;
        this.requestedAmount = requestedAmount;
        this.remainingTime = remainingTime;
        this.isQueueFirst = isQueueFirst;
        this.chargingPileId = chargingPileId;
        this.isAutoCompleted = isAutoCompleted;
        this.chargingPileStatus = chargingPileStatus;
        this.currentTotalFee = currentTotalFee;
        this.estimatedTotalFee = estimatedTotalFee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCurrentPower() {
        return currentPower;
    }

    public void setCurrentPower(Double currentPower) {
        this.currentPower = currentPower;
    }

    public Double getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(Double chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public Double getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Double totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Double getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(Double requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(Integer remainingTime) {
        this.remainingTime = remainingTime;
    }

    public Boolean getIsQueueFirst() {
        return isQueueFirst;
    }

    public void setIsQueueFirst(Boolean queueFirst) {
        isQueueFirst = queueFirst;
    }

    public Integer getChargingPileId() {
        return chargingPileId;
    }

    public void setChargingPileId(Integer chargingPileId) {
        this.chargingPileId = chargingPileId;
    }

    public Boolean getIsAutoCompleted() {
        return isAutoCompleted;
    }

    public void setIsAutoCompleted(Boolean autoCompleted) {
        isAutoCompleted = autoCompleted;
    }

    public String getChargingPileStatus() {
        return chargingPileStatus;
    }

    public void setChargingPileStatus(String chargingPileStatus) {
        this.chargingPileStatus = chargingPileStatus;
    }

    // 计费相关的getter和setter方法
    public BigDecimal getCurrentTotalFee() {
        return currentTotalFee;
    }

    public void setCurrentTotalFee(BigDecimal currentTotalFee) {
        this.currentTotalFee = currentTotalFee;
    }

    public BigDecimal getEstimatedTotalFee() {
        return estimatedTotalFee;
    }

    public void setEstimatedTotalFee(BigDecimal estimatedTotalFee) {
        this.estimatedTotalFee = estimatedTotalFee;
    }
} 