package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.mapper.SystemConstantMapper;
import com.example.backend.model.entity.SystemConstant;
import com.example.backend.service.SystemConstantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统常量服务实现类
 * 负责从数据库加载常量并在内存中管理
 */
@Service
public class SystemConstantServiceImpl implements SystemConstantService {
    @Autowired
    private SystemConstantMapper systemConstantMapper;
    
    // 内存中的常量值缓存
    private final ConcurrentHashMap<String, String> constantCache = new ConcurrentHashMap<>();
    
    // 内存中的常量类型缓存
    private final ConcurrentHashMap<String, String> constantTypeCache = new ConcurrentHashMap<>();
    
    @Override
    public void loadConstantsFromDatabase() {
        try {
            // 清空缓存
            constantCache.clear();
            constantTypeCache.clear();
            
            // 从数据库加载所有启用的常量
            List<SystemConstant> constants = systemConstantMapper.selectList(
                new LambdaQueryWrapper<SystemConstant>()
                    .eq(SystemConstant::getIsActive, true)
            );
            
            // 加载到内存缓存
            for (SystemConstant constant : constants) {
                constantCache.put(constant.getConstantKey(), constant.getConstantValue());
                constantTypeCache.put(constant.getConstantKey(), constant.getConstantType());
            }
        } catch (Exception e) {
        }
    }
    
    @Override
    public Object getConstant(String key) {
        String value = constantCache.get(key);
        String type = constantTypeCache.get(key);
        
        if (value == null || type == null) {
            return null;
        }
        
        return convertToType(value, type, key);
    }
    
    @Override
    public void updateConstantInMemory(String key, String value) {
        constantCache.put(key, value);
    }
    
    /**
     * 根据类型字符串将值转换为相应的类型
     */
    private Object convertToType(String value, String type, String key) {
        try {
            switch (type.toUpperCase()) {
                case "STRING":
                    return value;
                case "INTEGER":
                    return Integer.parseInt(value);
                case "DOUBLE":
                    return Double.parseDouble(value);
                case "BOOLEAN":
                    return Boolean.parseBoolean(value);
                default:
                    return value; // 默认返回字符串
            }
        } catch (Exception e) {
            return value; // 转换失败时返回原始字符串
        }
    }
} 