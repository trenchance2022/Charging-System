package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("charging_bill")
public class ChargingBill {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String billNumber;      // 详单编号，格式如 "BILL202312010001"
    private LocalDateTime billTime; // 详单生成时间
    private String requestId;       // 充电请求ID
    private Long userId;            // 用户ID
    private String pileNumber;      // 充电桩编号
    private Double chargedAmount;   // 充电电量，单位kWh
    private Integer chargingDuration; // 充电时长，单位分钟
    private LocalDateTime startTime;  // 启动时间
    private LocalDateTime stopTime;   // 停止时间
    private BigDecimal chargingFee;   // 充电费用，单位元
    private BigDecimal serviceFee;    // 服务费用，单位元
    private BigDecimal totalFee;      // 总费用，单位元
    private String chargingMode;      // 充电模式：fast/slow
    private Double chargingPower;     // 充电功率，单位kW

    public ChargingBill() {}

    public ChargingBill(String billNumber, String requestId, Long userId, String pileNumber, 
                       Double chargedAmount, Integer chargingDuration, LocalDateTime startTime, 
                       LocalDateTime stopTime, BigDecimal chargingFee, BigDecimal serviceFee, 
                       BigDecimal totalFee, String chargingMode, Double chargingPower) {
        this.billNumber = billNumber;
        this.billTime = LocalDateTime.now();
        this.requestId = requestId;
        this.userId = userId;
        this.pileNumber = pileNumber;
        this.chargedAmount = chargedAmount;
        this.chargingDuration = chargingDuration;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.chargingFee = chargingFee;
        this.serviceFee = serviceFee;
        this.totalFee = totalFee;
        this.chargingMode = chargingMode;
        this.chargingPower = chargingPower;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public LocalDateTime getBillTime() {
        return billTime;
    }

    public void setBillTime(LocalDateTime billTime) {
        this.billTime = billTime;
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

    public String getPileNumber() {
        return pileNumber;
    }

    public void setPileNumber(String pileNumber) {
        this.pileNumber = pileNumber;
    }

    public Double getChargedAmount() {
        return chargedAmount;
    }

    public void setChargedAmount(Double chargedAmount) {
        this.chargedAmount = chargedAmount;
    }

    public Integer getChargingDuration() {
        return chargingDuration;
    }

    public void setChargingDuration(Integer chargingDuration) {
        this.chargingDuration = chargingDuration;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public void setStopTime(LocalDateTime stopTime) {
        this.stopTime = stopTime;
    }

    public BigDecimal getChargingFee() {
        return chargingFee;
    }

    public void setChargingFee(BigDecimal chargingFee) {
        this.chargingFee = chargingFee;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public String getChargingMode() {
        return chargingMode;
    }

    public void setChargingMode(String chargingMode) {
        this.chargingMode = chargingMode;
    }

    public Double getChargingPower() {
        return chargingPower;
    }

    public void setChargingPower(Double chargingPower) {
        this.chargingPower = chargingPower;
    }
} 