package com.example.backend.service;

import com.example.backend.model.dto.QueueStatusDTO;

/**
 * 队列状态服务接口
 * 负责充电请求的队列状态查询和实时推送
 * 
 * @author System
 * @since 1.0
 */
public interface QueueService {
    
    /**
     * 根据请求ID获取队列状态
     * 包含排队号码、前方车辆数、预计等待时间等信息
     *
     * @param requestId 充电请求ID
     * @return 队列状态DTO，如果请求不存在或不在队列中返回空状态
     */
    QueueStatusDTO getQueueStatus(String requestId);
    
    /**
     * 根据用户ID获取其当前活跃请求的队列状态
     * 用户同时只能有一个活跃的充电请求
     *
     * @param userId 用户ID
     * @return 队列状态DTO，如果用户没有活跃请求返回空状态
     */
    QueueStatusDTO getUserQueueStatus(Long userId);
    
    /**
     * 启动队列状态推送定时任务
     * 每10秒执行一次，向所有排队中的用户推送最新队列状态
     * 包含排队号码、前方车辆数、预计等待时间的实时更新
     */
    void startQueuePushScheduler();
} 