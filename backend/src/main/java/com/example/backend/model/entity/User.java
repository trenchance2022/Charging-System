package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户实体类
 */
@TableName("user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    private String password;
    
    private String type; // 用户类型：admin(管理员)或user(普通用户)
    
    private Double batteryCapacity; // 电池容量（kWh）
    
    private Double currentPower; // 当前电量（kWh）
    
    // 构造函数
    public User() {}
    
    public User(String username, String password, String type) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.currentPower = 0.0; // 默认当前电量为0
    }
    
    public User(String username, String password, String type, Double batteryCapacity) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.batteryCapacity = batteryCapacity;
        this.currentPower = 0.0; // 默认当前电量为0
    }
    
    public User(String username, String password, String type, Double batteryCapacity, Double currentPower) {
        this.username = username;
        this.password = password;
        this.type = type;
        this.batteryCapacity = batteryCapacity;
        this.currentPower = currentPower;
    }
    
    // Getter和Setter
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Double getBatteryCapacity() {
        return batteryCapacity;
    }
    
    public void setBatteryCapacity(Double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }
    
    public Double getCurrentPower() {
        return currentPower;
    }
    
    public void setCurrentPower(Double currentPower) {
        this.currentPower = currentPower;
    }
} 