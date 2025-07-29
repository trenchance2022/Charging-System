package com.example.backend.model.dto;

public class ChargingRequestDTO {
    private String chargingMode;  // 充电模式：fast或slow
    private Double chargingAmount;  // 请求充电量，单位kWh

    public ChargingRequestDTO() {}

    public ChargingRequestDTO(String chargingMode, Double chargingAmount) {
        this.chargingMode = chargingMode;
        this.chargingAmount = chargingAmount;
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
} 