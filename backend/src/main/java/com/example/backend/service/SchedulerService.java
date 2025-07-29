package com.example.backend.service;

import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;

/**
 * 调度器服务接口
 * 实现智能调度算法，将等候区的充电请求分配到最优的充电桩
 */
public interface SchedulerService {
    
    /**
     * 为充电请求找到最优的充电桩
     * 调度策略：对应匹配充电模式下，被调度车辆完成充电所需时长最短
     * 
     * @param request 充电请求
     * @return 最优充电桩，如果没有合适的返回null
     */
    ChargingPile findOptimalPile(ChargingRequest request);
    
    /**
     * 计算在指定充电桩完成充电的总时长
     * 总时长 = 等待时间 + 自己充电时间
     * 
     * @param pile 充电桩
     * @param chargingAmount 请求充电量
     * @return 总时长（分钟）
     */
    int calculateTotalTime(ChargingPile pile, Double chargingAmount);
    
    /**
     * 计算在指定充电桩的等待时间
     * 等待时间 = 选定充电桩队列中所有车辆完成充电时间之和
     * 
     * @param pile 充电桩
     * @return 等待时间（分钟）
     */
    int calculateWaitingTime(ChargingPile pile);
    
    /**
     * 计算充电时间
     * 充电时间 = 请求充电量 / 充电桩功率
     * 
     * @param chargingAmount 充电量（kWh）
     * @param chargingPower 充电功率（kW）
     * @return 充电时间（分钟）
     */
    int calculateChargingTime(Double chargingAmount, Double chargingPower);
    
    /**
     * 处理等候区的充电请求
     * 定期扫描等候区，为未分配充电桩的请求分配最优充电桩
     */
    void processWaitingRequests();
} 