package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.SystemConfigMapper;
import com.example.backend.model.entity.SystemConfig;
import com.example.backend.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现类
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> 
        implements SystemConfigService {
    @Override
    public List<SystemConfig> getAllConfigs() {
        return list();
    }
    
    @Override
    public Map<String, String> getConfigMap() {
        List<SystemConfig> configs = getAllConfigs();
        Map<String, String> configMap = new HashMap<>();
        
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        
        return configMap;
    }
    
    @Override
    @Transactional
    public boolean updateConfigs(Map<String, String> configMap) {
        try {
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // 查找现有配置
                LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(SystemConfig::getConfigKey, key);
                SystemConfig existingConfig = getOne(queryWrapper);
                
                if (existingConfig != null) {
                    // 更新现有配置
                    existingConfig.setConfigValue(value);
                    updateById(existingConfig);
                } else {
                    // 创建新配置
                    SystemConfig newConfig = new SystemConfig(key, value, "系统配置");
                    save(newConfig);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemConfig::getConfigKey, configKey);
        SystemConfig config = getOne(queryWrapper);
        
        return config != null ? config.getConfigValue() : null;
    }
    
    @Override
    @Transactional
    public boolean updateConfig(String configKey, String configValue) {
        try {
            LambdaQueryWrapper<SystemConfig> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemConfig::getConfigKey, configKey);
            SystemConfig existingConfig = getOne(queryWrapper);
            
            if (existingConfig != null) {
                existingConfig.setConfigValue(configValue);
                return updateById(existingConfig);
            } else {
                SystemConfig newConfig = new SystemConfig(configKey, configValue, "系统配置");
                return save(newConfig);
            }
        } catch (Exception e) {
            return false;
        }
    }
} 