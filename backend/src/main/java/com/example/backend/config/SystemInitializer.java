package com.example.backend.config;

import com.example.backend.service.ChargingPileMonitorService;
import com.example.backend.service.SystemConstantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 系统初始化器
 * 在应用启动时执行必要的初始化操作
 * 
 * @author System
 * @since 1.0
 */
@Component
public class SystemInitializer implements ApplicationRunner {
    @Autowired
    private SystemConstantService systemConstantService;
    
    @Autowired
    private ChargingPileMonitorService chargingPileMonitorService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 加载系统常量到内存
        initializeSystemConstants();
        
        // 初始化充电桩状态缓存
        initializePileStatusCache();
    }
    
    /**
     * 初始化系统常量
     * 从数据库加载所有系统常量到内存缓存中
     */
    private void initializeSystemConstants() {
        try {
            systemConstantService.loadConstantsFromDatabase();
        } catch (Exception e) {
        }
    }
    
    /**
     * 初始化充电桩状态缓存
     * 记录所有充电桩的初始状态，为后续状态变化监控做准备
     */
    private void initializePileStatusCache() {
        try {
            chargingPileMonitorService.initializePileStatusCache();
        } catch (Exception e) {
        }
    }
} 