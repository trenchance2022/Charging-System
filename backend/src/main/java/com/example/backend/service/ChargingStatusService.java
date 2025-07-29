package com.example.backend.service;

/**
 * 充电状态服务接口
 * 负责充电状态的实时监控、自动停止判断和状态推送
 * 
 * 主要功能：
 * - 每10秒检查所有充电中的请求
 * - 自动停止已达到目标充电量的请求
 * - 实时推送充电状态更新给客户端
 * 
 * @author System
 * @since 1.0
 */
public interface ChargingStatusService {
    
    /**
     * 启动充电状态监控定时任务
     * 每10秒执行一次，检查所有充电中的请求：
     * - 判断是否达到目标充电量，自动停止充电
     * - 推送实时充电状态给连接的客户端
     * - 处理充电完成后的后续流程
     */
    void startStatusPushScheduler();
    
    /**
     * 推送充电状态更新到客户端
     * 用于在充电状态发生变化时主动推送给用户
     * 
     * @param requestId 充电请求ID
     */
    void pushChargingStatusUpdate(String requestId);
} 