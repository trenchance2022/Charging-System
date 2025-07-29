package com.example.backend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统常量实体类
 * 用于存储系统中使用的各种常量值
 */
@TableName("system_constant")
public class SystemConstant {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String constantKey;     // 常量键名
    private String constantValue;   // 常量值
    private String constantType;    // 常量类型 (STRING, INTEGER, DOUBLE, BOOLEAN)
    private String description;     // 常量描述
    private Boolean isActive;       // 是否启用
    
    public SystemConstant() {}
    
    public SystemConstant(String constantKey, String constantValue, String constantType, 
                         String description, Boolean isActive) {
        this.constantKey = constantKey;
        this.constantValue = constantValue;
        this.constantType = constantType;
        this.description = description;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getConstantKey() {
        return constantKey;
    }
    
    public void setConstantKey(String constantKey) {
        this.constantKey = constantKey;
    }
    
    public String getConstantValue() {
        return constantValue;
    }
    
    public void setConstantValue(String constantValue) {
        this.constantValue = constantValue;
    }
    
    public String getConstantType() {
        return constantType;
    }
    
    public void setConstantType(String constantType) {
        this.constantType = constantType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
} 