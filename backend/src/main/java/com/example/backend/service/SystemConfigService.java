package com.example.backend.service;

import com.example.backend.model.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 * 负责管理系统配置参数的获取和更新
 */
public interface SystemConfigService {
    
    /**
     * 获取所有系统配置
     * @return 系统配置列表
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * 获取系统配置的键值对格式
     * @return 配置键值对映射
     */
    Map<String, String> getConfigMap();
    
    /**
     * 更新系统配置
     * @param configMap 配置键值对映射
     * @return 更新是否成功
     */
    boolean updateConfigs(Map<String, String> configMap);
    
    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值，如果不存在返回null
     */
    String getConfigValue(String configKey);
    
    /**
     * 更新单个配置
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 更新是否成功
     */
    boolean updateConfig(String configKey, String configValue);
} 