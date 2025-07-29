package com.example.backend.service;

import com.example.backend.model.entity.ChargingPile;

/**
 * 充电桩监控服务接口
 * 负责监控充电桩状态变化并处理相应的业务逻辑
 * 
 * 监控的状态变化包括：
 * - 可用 -> 不可用：充电桩故障或维护
 * - 充电中 -> 不可用：充电过程中出现故障
 * - 不可用 -> 可用：充电桩恢复正常
 * 
 * @author System
 * @since 1.0
 */
public interface ChargingPileMonitorService {
    
    /**
     * 初始化充电桩状态缓存
     * 在系统启动时调用，记录所有充电桩的初始状态
     */
    void initializePileStatusCache();
    
    /**
     * 启动充电桩状态监控定时任务
     * 定期检查所有充电桩的状态变化，发现变化时触发相应处理逻辑
     */
    void monitorPileStatusChanges();
    
    /**
     * 处理充电桩状态变化
     * 根据状态变化类型调用相应的处理方法
     * 
     * @param pile 发生状态变化的充电桩
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    void handleStatusChange(ChargingPile pile, String oldStatus, String newStatus);
    
    /**
     * 处理充电桩从可用变为不可用的情况
     * 需要处理：队列车辆重新分配、通知等待用户等
     * 
     * @param pile 发生状态变化的充电桩
     */
    void handleAvailableToUnavailable(ChargingPile pile);
    
    /**
     * 处理充电桩从充电中变为不可用的情况
     * 需要处理：当前充电车辆处理、队列车辆重新分配、紧急通知等
     * 
     * @param pile 发生状态变化的充电桩
     */
    void handleChargingToUnavailable(ChargingPile pile);
    
    /**
     * 处理充电桩从不可用变为可用的情况
     * 需要处理：重新加入调度池、通知等候区车辆等
     * 
     * @param pile 发生状态变化的充电桩
     */
    void handleUnavailableToAvailable(ChargingPile pile);
} 