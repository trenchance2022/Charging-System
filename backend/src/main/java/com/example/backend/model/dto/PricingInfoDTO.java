package com.example.backend.model.dto;

import java.math.BigDecimal;

/**
 * 分时电价信息DTO
 */
public class PricingInfoDTO {
    private String priceType;        // 电价类型：PEAK/NORMAL/VALLEY
    private String priceTypeName;    // 电价类型名称：峰时/平时/谷时
    private BigDecimal unitPrice;    // 单位电价（元/度）
    private BigDecimal serviceFeeRate; // 服务费率（元/度）
    private String timePeriods;      // 时间段描述
    private String currentPeriod;    // 当前时段描述

    public PricingInfoDTO() {}

    public PricingInfoDTO(String priceType, String priceTypeName, BigDecimal unitPrice, 
                         BigDecimal serviceFeeRate, String timePeriods, String currentPeriod) {
        this.priceType = priceType;
        this.priceTypeName = priceTypeName;
        this.unitPrice = unitPrice;
        this.serviceFeeRate = serviceFeeRate;
        this.timePeriods = timePeriods;
        this.currentPeriod = currentPeriod;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public String getPriceTypeName() {
        return priceTypeName;
    }

    public void setPriceTypeName(String priceTypeName) {
        this.priceTypeName = priceTypeName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getServiceFeeRate() {
        return serviceFeeRate;
    }

    public void setServiceFeeRate(BigDecimal serviceFeeRate) {
        this.serviceFeeRate = serviceFeeRate;
    }

    public String getTimePeriods() {
        return timePeriods;
    }

    public void setTimePeriods(String timePeriods) {
        this.timePeriods = timePeriods;
    }

    public String getCurrentPeriod() {
        return currentPeriod;
    }

    public void setCurrentPeriod(String currentPeriod) {
        this.currentPeriod = currentPeriod;
    }
} 