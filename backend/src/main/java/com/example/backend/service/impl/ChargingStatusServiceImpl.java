package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.infrastructure.sse.SseManager;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.dto.ChargingStatusDTO;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingStatusService;
import com.example.backend.service.ChargingPileService;
import com.example.backend.service.ChargingRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 充电状态服务实现类
 * 
 * 实现功能：
 * - 状态监控：每10秒检查所有充电中的请求状态
 * - 自动停止：当达到目标充电量时自动停止充电
 * - 实时推送：向连接的客户端推送充电状态更新
 * - 后续处理：处理充电完成后的调度和状态更新
 * 
 * @author System
 * @since 1.0
 */
@Service
@EnableScheduling
public class ChargingStatusServiceImpl implements ChargingStatusService {
    @Autowired
    private ChargingRequestMapper chargingRequestMapper;
    
    @Autowired
    private ChargingPileService chargingPileService;
    
    @Autowired
    @Lazy
    private ChargingRequestService chargingRequestService;
    
    @Autowired
    private SseManager sseManager;
    
    /**
     * 启动充电状态监控定时任务
     * 每5秒执行一次，向所有活跃请求推送状态更新，并检查是否需要自动停止充电
     */
    @Scheduled(fixedRate = 5000)
    @Override
    @Transactional
    public void startStatusPushScheduler() {
        // 获取所有正在充电的请求
        List<ChargingRequest> chargingRequests = getChargingRequests();
        
        for (ChargingRequest request : chargingRequests) {
            String requestId = request.getRequestId();
            
            // 检查是否需要自动停止充电
            if (shouldAutoStopCharging(request)) {
                // 方法内部会自动推送完成状态到前端
                chargingRequestService.autoStopCharging(requestId);
            } else {
                // 正常充电中，推送实时状态更新
                pushChargingStatusUpdate(requestId);
            }
        }
        
        // 获取所有等待中的请求并推送状态更新
        List<ChargingRequest> waitingRequests = getWaitingRequests();
        
        for (ChargingRequest request : waitingRequests) {
            // 推送等待状态更新
            pushChargingStatusUpdate(request.getRequestId());
        }
    }
    
    /**
     * 推送充电状态更新
     * 整合了原本在控制器层的推送逻辑，直接推送给用户客户端
     * 
     * @param requestId 充电请求ID
     */
    @Override
    public void pushChargingStatusUpdate(String requestId) {
        try {
            // 获取最新状态
            ChargingStatusDTO statusDTO = chargingRequestService.getChargingStatus(requestId);
            if (statusDTO == null) {
                return;
            }
            
            // 获取请求信息以找到用户ID
            ChargingRequest request = chargingRequestService.getRequestByRequestId(requestId);
            if (request != null && request.getUserId() != null) {
                String userKey = "charge_user_" + request.getUserId();
                
                // 推送给用户
                sseManager.sendToEmitter(userKey, statusDTO);
            }
        } catch (Exception e) {
        }
    }
    
    /**
     * 检查是否应该自动停止充电
     * 判断标准：实际充电量是否达到或超过请求的充电量
     * 
     * @param request 充电请求
     * @return 是否应该自动停止
     */
    private boolean shouldAutoStopCharging(ChargingRequest request) {
        if (request.getStartTime() == null || request.getChargingPileId() == null) {
            return false;
        }
        
        // 获取充电桩信息
        ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
        if (pile == null) {
            return false;
        }
        
        // 计算已充电时间（分钟）
        LocalDateTime now = LocalDateTime.now();
        long chargedMinutes = ChronoUnit.MINUTES.between(request.getStartTime(), now);
        
        // 计算已充电量 = 充电功率 * 充电时间 / 60
        double chargedAmount = (pile.getChargingPower() * chargedMinutes) / 60.0;
        
        // 如果已充电量达到或超过请求的充电量，则应该自动停止
        return chargedAmount >= request.getChargingAmount();
    }
    
    /**
     * 获取所有正在充电的请求
     *
     * @return 充电请求列表
     */
    private List<ChargingRequest> getChargingRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_CHARGING);
        return chargingRequestMapper.selectList(queryWrapper);
    }

    /**
     * 获取所有等待中的请求
     *
     * @return 等待中的请求列表
     */
    private List<ChargingRequest> getWaitingRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                   .or()
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING);
        return chargingRequestMapper.selectList(queryWrapper);
    }
} 