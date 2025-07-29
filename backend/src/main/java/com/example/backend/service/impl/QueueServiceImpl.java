package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.infrastructure.sse.SseManager;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.dto.QueueStatusDTO;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingRequestService;
import com.example.backend.service.QueueService;
import com.example.backend.service.SystemConstantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 队列状态服务实现类
 * 
 * 实现功能：
 * - 队列状态查询：包含排队号码、前方车辆数、预计等待时间
 * - 定时状态推送：每10秒向所有排队用户推送最新队列状态
 * - 等待时间估算：基于前方车辆的充电需求进行智能估算
 * 
 * @author System
 * @since 1.0
 */
@Service
@EnableScheduling
public class QueueServiceImpl implements QueueService {
    @Autowired
    private ChargingRequestMapper chargingRequestMapper;
    
    @Autowired
    private ChargingRequestService chargingRequestService;

    @Autowired
    private SystemConstantService systemConstantService;

    @Autowired
    private SseManager sseManager;
    
    /**
     * 根据请求ID获取队列状态
     * 
     * @param requestId 充电请求ID
     * @return 队列状态DTO，包含排队号码、前方车辆数、预计等待时间等信息
     */
    @Override
    public QueueStatusDTO getQueueStatus(String requestId) {
        ChargingRequest request = chargingRequestService.getRequestByRequestId(requestId);
        
        if (request == null) {
            return new QueueStatusDTO("NONE", 0, 0, "NONE");
        }
        
        // 如果不是等待状态或优先等待状态，返回空队列信息
        if (!ChargingConstants.STATUS_WAITING.equals(request.getStatus()) && 
            !ChargingConstants.STATUS_PRIORITY_WAITING.equals(request.getStatus())) {
            return new QueueStatusDTO("NONE", 0, 0, "NONE");
        }
        
        // 直接从请求对象获取全局排队号码
        String queueNumber = request.getRequestId();
        
        // 获取队列信息
        Integer chargingPileId = request.getChargingPileId();
        String locationType;
        
        // 确定位置类型（等候区或充电桩队列）
        if (chargingPileId != null) {
            locationType = "CHARGING_PILE";
        } else {
            locationType = "WAITING_AREA";
        }
        
        // 获取前方等待车辆数
        int queueCount = getQueueCountBefore(request);
        int estimatedWait = calculateEstimatedWaitTime(queueCount);
        
        return new QueueStatusDTO(queueNumber, queueCount, estimatedWait, locationType);
    }
    
    /**
     * 根据用户ID获取其当前活跃请求的队列状态
     * 
     * @param userId 用户ID
     * @return 队列状态DTO，如果用户没有活跃请求返回空状态
     */
    @Override
    public QueueStatusDTO getUserQueueStatus(Long userId) {
        // 获取用户的活跃请求
        ChargingRequest request = chargingRequestService.getActiveRequest(userId);
        if (request == null) {
            return new QueueStatusDTO("NONE", 0, 0, "NONE");
        }
        
        // 使用现有的getQueueStatus方法
        return getQueueStatus(request.getRequestId());
    }
    
    /**
     * 定时队列状态推送任务
     * 每5秒执行一次，向所有排队中的用户推送最新队列状态
     */
    @Scheduled(fixedRate = 5000)
    public void startQueuePushScheduler() {
        // 获取所有排队的请求
        List<ChargingRequest> queueRequests = getQueueRequests();
        
        // 直接推送每个请求的状态更新
        for (ChargingRequest request : queueRequests) {
            pushQueueStatusUpdate(request.getRequestId());
        }
    }
    
    /**
     * 推送队列状态更新到指定用户
     * 整合了SSE推送逻辑，同时推送队列状态和充电状态给用户客户端
     * 
     * @param requestId 充电请求ID
     */
    private void pushQueueStatusUpdate(String requestId) {
        // 获取最新队列状态
        QueueStatusDTO queueStatusDTO = getQueueStatus(requestId);
        if (queueStatusDTO == null) {
            return;
        }
        
        // 获取请求信息以找到用户ID
        ChargingRequest request = chargingRequestService.getRequestByRequestId(requestId);
        
        if (request != null && request.getUserId() != null) {
            String queueUserKey = "queue_user_" + request.getUserId();
            
            // 推送队列状态到队列SSE连接
            sseManager.sendToEmitter(queueUserKey, queueStatusDTO);
        }
    }
    
    /**
     * 获取请求前面的排队数量
     * 
     * @param request 充电请求
     * @return 前方排队车辆数
     */
    private int getQueueCountBefore(ChargingRequest request) {
        if (request == null || (!ChargingConstants.STATUS_WAITING.equals(request.getStatus()) && 
                                !ChargingConstants.STATUS_PRIORITY_WAITING.equals(request.getStatus()))) {
            return 0;
        }
        
        // 如果已经分配了充电桩，返回队列位置-1（前面的车辆数）
        if (request.getChargingPileId() != null && request.getQueuePosition() != null) {
            return request.getQueuePosition() - 1;
        }
        
        // 如果还未分配充电桩，计算在等候区中的位置
        // 优先队列逻辑：PRIORITY_WAITING 全部排在 WAITING 前面，同类内部按创建时间排序
        
        String currentStatus = request.getStatus();
        LocalDateTime currentCreateTime = request.getCreateTime();
        
        int queueCount = 0;
        
        if (ChargingConstants.STATUS_PRIORITY_WAITING.equals(currentStatus)) {
            // 如果当前请求是优先等待，只计算创建时间更早的优先等待请求
            LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                       .lt(ChargingRequest::getCreateTime, currentCreateTime);
            
            Long countValue = chargingRequestMapper.selectCount(queryWrapper);
            queueCount = countValue.intValue();
            
        } else if (ChargingConstants.STATUS_WAITING.equals(currentStatus)) {
            // 如果当前请求是普通等待，前面包括：所有优先等待 + 创建时间更早的普通等待
            
            // 1. 统计所有优先等待的请求
            LambdaQueryWrapper<ChargingRequest> priorityQueryWrapper = new LambdaQueryWrapper<>();
            priorityQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING);
            Long priorityCount = chargingRequestMapper.selectCount(priorityQueryWrapper);
            
            // 2. 统计创建时间更早的普通等待请求
            LambdaQueryWrapper<ChargingRequest> normalQueryWrapper = new LambdaQueryWrapper<>();
            normalQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                             .lt(ChargingRequest::getCreateTime, currentCreateTime);
            Long normalCount = chargingRequestMapper.selectCount(normalQueryWrapper);
            
            queueCount = priorityCount.intValue() + normalCount.intValue();
        }
        
        return queueCount;
    }
    
    /**
     * 获取所有排队的请求
     *
     * @return 排队请求列表
     */
    private List<ChargingRequest> getQueueRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING, ChargingConstants.STATUS_PRIORITY_WAITING);
        return chargingRequestMapper.selectList(queryWrapper);
    }
    
    /**
     * 计算预计等待时间
     * 基于前方车辆的充电需求进行估算
     *
     * @param queueCount 前方车辆数
     * @return 预计等待时间（分钟）
     */
    private int calculateEstimatedWaitTime(int queueCount) {
        // 如果没有排队车辆，等待时间为0
        if (queueCount <= 0) {
            return 0;
        }
        
        // 获取等待中和优先等待中的请求，按照优先队列逻辑排序
        // 1. 先获取所有优先等待的请求，按创建时间排序
        LambdaQueryWrapper<ChargingRequest> priorityQueryWrapper = new LambdaQueryWrapper<>();
        priorityQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                            .orderBy(true, true, ChargingRequest::getCreateTime);
        List<ChargingRequest> priorityRequests = chargingRequestMapper.selectList(priorityQueryWrapper);
        
        // 2. 再获取所有普通等待的请求，按创建时间排序
        LambdaQueryWrapper<ChargingRequest> normalQueryWrapper = new LambdaQueryWrapper<>();
        normalQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                            .orderBy(true, true, ChargingRequest::getCreateTime);
        List<ChargingRequest> normalRequests = chargingRequestMapper.selectList(normalQueryWrapper);
        
        // 3. 合并队列：优先等待 + 普通等待
        List<ChargingRequest> waitingRequests = new ArrayList<>();
        waitingRequests.addAll(priorityRequests);
        waitingRequests.addAll(normalRequests);
        
        int totalWaitMinutes = 0;
        
        // 只计算前方等待请求的预计充电时间
        for (ChargingRequest request : waitingRequests) {
            // 只计算排在前面的请求
            if (queueCount > 0) {
                String mode = request.getChargingMode();
                
                double power = ChargingConstants.MODE_FAST.equals(mode) 
                        ? (double) systemConstantService.getConstant(ChargingConstants.CONFIG_FAST_CHARGING_POWER)
                        : (double) systemConstantService.getConstant(ChargingConstants.CONFIG_SLOW_CHARGING_POWER);
                
                Double chargingAmount = request.getChargingAmount();
                if (chargingAmount == null) continue; // 跳过没有充电量的请求
                
                // 计算充电时间（分钟）
                int chargeMinutes = (int) Math.ceil((chargingAmount * 60) / power);
                totalWaitMinutes += chargeMinutes;
                
                queueCount--;
            } else {
                break;
            }
        }
        
        return Math.max(totalWaitMinutes, 1); // 至少返回1分钟
    }
} 