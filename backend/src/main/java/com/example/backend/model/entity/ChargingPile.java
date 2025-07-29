package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("charging_pile")
public class ChargingPile {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String pileNumber;  // 充电桩编号，如 "F001", "T001"
    private String pileType;    // 充电桩类型：FAST(快充) 或 SLOW(慢充)
    private Double chargingPower;  // 充电功率，单位kW
    private String status;      // 充电桩状态：AVAILABLE(可用)、CHARGING(充电中)、UNAVAILABLE(不可用)

    public ChargingPile() {}

    public ChargingPile(String pileNumber, String pileType, Double chargingPower, String status) {
        this.pileNumber = pileNumber;
        this.pileType = pileType;
        this.chargingPower = chargingPower;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPileNumber() {
        return pileNumber;
    }

    public void setPileNumber(String pileNumber) {
        this.pileNumber = pileNumber;
    }

    public String getPileType() {
        return pileType;
    }

    public void setPileType(String pileType) {
        this.pileType = pileType;
    }

    public Double getChargingPower() {
        return chargingPower;
    }

    public void setChargingPower(Double chargingPower) {
        this.chargingPower = chargingPower;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 