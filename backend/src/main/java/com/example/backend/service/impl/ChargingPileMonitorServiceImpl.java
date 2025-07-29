package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.infrastructure.sse.SseManager;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.dto.PileFailureNotificationDTO;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingPileMonitorService;
import com.example.backend.service.ChargingPileService;
import com.example.backend.service.ChargingBillService;
import com.example.backend.service.ChargingRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 充电桩监控服务实现类
 * 
 * 实现功能：
 * - 状态监控：每10秒检查所有充电桩的状态变化
 * - 状态变化处理：根据不同的状态变化类型执行相应的业务逻辑
 * - 故障恢复：处理充电桩故障和恢复场景
 * 
 * @author System
 * @since 1.0
 */
@Service
@EnableScheduling
public class ChargingPileMonitorServiceImpl implements ChargingPileMonitorService {

    @Autowired
    private ChargingPileService chargingPileService;
    
    @Autowired
    private ChargingRequestMapper chargingRequestMapper;
    
    @Autowired
    private SseManager sseManager;
    
    @Autowired
    private ChargingBillService chargingBillService;
    
    @Autowired
    private ChargingRequestService chargingRequestService;
    
    // 缓存充电桩的上一次状态，用于检测状态变化
    private final Map<Integer, String> lastKnownStatus = new ConcurrentHashMap<>();

    /**
     * 初始化充电桩状态缓存
     * 在系统启动时调用，记录所有充电桩的初始状态
     */
    @Override
    public void initializePileStatusCache() {
        List<ChargingPile> allPiles = chargingPileService.getAllPiles();
        
        for (ChargingPile pile : allPiles) {
            lastKnownStatus.put(pile.getId(), pile.getStatus());
        }
    }

    /**
     * 充电桩状态监控定时任务
     * 每5秒执行一次，检查所有充电桩的状态变化
     */
    @Scheduled(fixedRate = 5000)
    @Override
    public void monitorPileStatusChanges() {
        // 获取所有充电桩
        List<ChargingPile> allPiles = chargingPileService.getAllPiles();
        
        for (ChargingPile pile : allPiles) {
            Integer pileId = pile.getId();
            String currentStatus = pile.getStatus();
            String previousStatus = lastKnownStatus.get(pileId);
            
            // 只处理状态发生变化的情况
            if (previousStatus != null && !previousStatus.equals(currentStatus)) {
                // 处理状态变化
                handleStatusChange(pile, previousStatus, currentStatus);
                
                // 更新缓存中的状态
                lastKnownStatus.put(pileId, currentStatus);
            }
        }
    }

    /**
     * 处理充电桩状态变化
     * 根据状态变化类型调用相应的处理方法
     * 
     * @param pile 发生状态变化的充电桩
     * @param oldStatus 原状态
     * @param newStatus 新状态
     */
    @Override
    public void handleStatusChange(ChargingPile pile, String oldStatus, String newStatus) {
        // 可用 -> 不可用
        if (ChargingConstants.PILE_STATUS_AVAILABLE.equals(oldStatus) && 
            ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(newStatus)) {
            handleAvailableToUnavailable(pile);
        }
        // 充电中 -> 不可用
        else if (ChargingConstants.PILE_STATUS_CHARGING.equals(oldStatus) && 
                    ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(newStatus)) {
            handleChargingToUnavailable(pile);
        }
        // 不可用 -> 可用
        else if (ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(oldStatus) && 
                    ChargingConstants.PILE_STATUS_AVAILABLE.equals(newStatus)) {
            handleUnavailableToAvailable(pile);
        }
    }

    /**
     * 处理充电桩从可用变为不可用的情况
     * 将队列中的WAITING请求改为PRIORITY_WAITING并放回等候区
     * 
     * @param pile 发生状态变化的充电桩
     */
    @Override
    public void handleAvailableToUnavailable(ChargingPile pile) {
        // 直接调用处理队列车辆的方法，避免重复代码
        handleQueueWaitingVehicles(pile.getId());
    }
    
    /**
     * 发送充电桩故障通知给用户
     * 
     * @param request 受影响的充电请求
     * @param pile 故障的充电桩
     */
    private void sendFailureNotification(ChargingRequest request, ChargingPile pile) {
        try {
            // 构建通知消息
            PileFailureNotificationDTO notification = new PileFailureNotificationDTO(
                "PILE_FAILURE",
                pile.getPileNumber(),
                request.getRequestId(),
                "充电桩故障通知",
                String.format("抱歉，您所排队的充电桩 %s 发生故障已暂停使用。您的充电请求已被重新优先调度，将在其他充电桩可用时优先安排。", 
                    pile.getPileNumber()),
                "WARNING"
            );
            
            // 向用户发送通知
            String notificationKey = "notification_user_" + request.getUserId();
            sseManager.sendToEmitter(notificationKey, notification);
            
        } catch (Exception e) {
        }
    }

    /**
     * 处理充电桩从充电中变为不可用的情况
     * 需要处理：当前充电车辆处理、队列车辆重新分配、紧急通知等
     * 
     * @param pile 发生状态变化的充电桩
     */
    @Override
    public void handleChargingToUnavailable(ChargingPile pile) {
        // 1. 处理当前正在充电的车辆
        ChargingRequest currentChargingRequest = getCurrentChargingRequest(pile.getId());
        if (currentChargingRequest != null) {
            handleCurrentChargingVehicle(currentChargingRequest, pile);
        }
        
        // 2. 处理队列中等待的车辆
        handleQueueWaitingVehicles(pile.getId());
        
        // 3. 更新充电桩状态为不可用（如果还没有更新的话）
        if (!ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(pile.getStatus())) {
            pile.setStatus(ChargingConstants.PILE_STATUS_UNAVAILABLE);
            // 这里不需要更新，因为调用此方法的地方已经处理了状态更新
        }
    }
    
    /**
     * 处理当前正在充电的车辆
     * 停止计费、生成详单、计算剩余电量并重新生成优先请求
     * 
     * @param chargingRequest 正在充电的请求
     * @param pile 故障的充电桩
     */
    private void handleCurrentChargingVehicle(ChargingRequest chargingRequest, ChargingPile pile) {
        try {
            // 先计算实际充电量
            Double actualChargedAmount = 0.0;
            if (chargingRequest.getStartTime() != null) {
                LocalDateTime now = LocalDateTime.now();
                long chargedMinutes = ChronoUnit.MINUTES.between(chargingRequest.getStartTime(), now);
                actualChargedAmount = (pile.getChargingPower() * chargedMinutes) / 60.0;
                actualChargedAmount = Math.min(actualChargedAmount, chargingRequest.getChargingAmount());
            }
            
            // 更新用户当前电量
            chargingRequestService.updateUserCurrentPowerAfterCharging(chargingRequest);
            
            // 停止充电并更新请求状态
            chargingRequest.setStatus(ChargingConstants.STATUS_COMPLETED);
            chargingRequest.setEndTime(LocalDateTime.now());
            chargingRequestMapper.updateById(chargingRequest);
            
            // 生成充电详单
            try {
                chargingBillService.generateBill(chargingRequest);
            } catch (Exception e) {
            }
            
            // 计算剩余充电量
            Double remainingAmount = chargingRequest.getChargingAmount() - actualChargedAmount;
            
            // 如果还有剩余电量需要充电，生成新的优先等待请求
            if (remainingAmount >= 0.5) { // 剩余电量大于等于0.5kWh才重新生成请求
                createPriorityRequest(chargingRequest, remainingAmount, pile);
            }
            
            // 发送故障通知给当前充电用户
            sendChargingInterruptionNotification(chargingRequest, pile, actualChargedAmount, remainingAmount);
            
        } catch (Exception e) {
        }
    }
    
    /**
     * 处理队列中等待的车辆
     * 将所有等待中的请求改为优先等待并放回等候区
     * 
     * @param pileId 故障充电桩ID
     */
    private void handleQueueWaitingVehicles(Integer pileId) {
        // 获取充电桩对象
        ChargingPile pile = chargingPileService.getById(pileId);
        if (pile == null) {
            return; // 充电桩不存在，直接返回
        }
        
        // 获取该充电桩队列中的所有等待请求
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING);
        
        List<ChargingRequest> waitingRequests = chargingRequestMapper.selectList(queryWrapper);
        
        // 将这些请求改为优先级等待状态并放回等候区
        for (ChargingRequest request : waitingRequests) {
            // 使用UpdateWrapper来明确更新null值
            LambdaUpdateWrapper<ChargingRequest> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(ChargingRequest::getId, request.getId())
                        .set(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                        .set(ChargingRequest::getChargingPileId, null)  // 明确设置为null
                        .set(ChargingRequest::getQueuePosition, null);  // 明确设置为null
            
            // 更新数据库
            chargingRequestMapper.update(null, updateWrapper);
            
            // 发送故障通知给用户
            sendFailureNotification(request, pile);
        }
    }
    
    /**
     * 获取当前在指定充电桩充电的请求
     * 
     * @param pileId 充电桩ID
     * @return 当前充电的请求，如果没有则返回null
     */
    private ChargingRequest getCurrentChargingRequest(Integer pileId) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_CHARGING);
        
        List<ChargingRequest> chargingRequests = chargingRequestMapper.selectList(queryWrapper);
        return chargingRequests.isEmpty() ? null : chargingRequests.get(0);
    }
    
    /**
     * 创建新的优先等待请求
     * 
     * @param originalRequest 原始请求
     * @param remainingAmount 剩余充电量
     * @param pile 故障充电桩
     */
    private void createPriorityRequest(ChargingRequest originalRequest, Double remainingAmount, ChargingPile pile) {
        try {
            // 生成新的请求ID
            String chargingMode = originalRequest.getChargingMode();
            String newRequestId = chargingRequestService.generateRequestId(chargingMode);
            
            // 创建新的优先等待请求
            ChargingRequest newRequest = new ChargingRequest();
            newRequest.setRequestId(newRequestId);
            newRequest.setUserId(originalRequest.getUserId());
            newRequest.setChargingMode(originalRequest.getChargingMode());
            newRequest.setChargingAmount(remainingAmount);
            newRequest.setStatus(ChargingConstants.STATUS_PRIORITY_WAITING);
            newRequest.setCreateTime(LocalDateTime.now());
            // 确保在等候区（不分配充电桩）
            newRequest.setChargingPileId(null);
            newRequest.setQueuePosition(null);
            
            // 保存新请求
            chargingRequestMapper.insert(newRequest);
            
        } catch (Exception e) {
        }
    }
    
    /**
     * 发送充电中断通知给用户
     * 
     * @param request 中断的充电请求
     * @param pile 故障的充电桩
     * @param actualChargedAmount 实际充电量
     * @param remainingAmount 剩余充电量
     */
    private void sendChargingInterruptionNotification(ChargingRequest request, ChargingPile pile, 
                                                     Double actualChargedAmount, Double remainingAmount) {
        try {
            String message;
            if (remainingAmount >= 0.5) {
                message = String.format("充电桩 %s 发生故障，您的充电已中断。已充电 %.1f kWh，剩余 %.1f kWh 已自动重新排队（优先处理）。", 
                    pile.getPileNumber(), actualChargedAmount, remainingAmount);
            } else {
                message = String.format("充电桩 %s 发生故障，您的充电已完成并中断。已充电 %.1f kWh。", 
                    pile.getPileNumber(), actualChargedAmount);
            }
            
            // 构建通知消息
            PileFailureNotificationDTO notification = new PileFailureNotificationDTO(
                "CHARGING_INTERRUPTION",
                pile.getPileNumber(),
                request.getRequestId(),
                "充电中断通知",
                message,
                "WARNING"
            );
            
            // 向用户发送通知
            String notificationKey = "notification_user_" + request.getUserId();
            sseManager.sendToEmitter(notificationKey, notification);
            
        } catch (Exception e) {
        }
    }

    /**
     * 处理充电桩从不可用变为可用的情况
     * 需要处理：重新加入调度池、通知等候区车辆等
     * 
     * @param pile 发生状态变化的充电桩
     */
    @Override
    public void handleUnavailableToAvailable(ChargingPile pile) {
        try {
            // 获取故障恢复充电桩的类型
            String pileType = pile.getPileType();
            
            // 查找所有同类型的其他充电桩（排除当前恢复的充电桩）
            List<ChargingPile> otherSameTypePiles = chargingPileService.getPilesByType(pileType)
                    .stream()
                    .filter(p -> !p.getId().equals(pile.getId()))
                    .collect(Collectors.toList());
            
            // 检查其他同类型充电桩中是否有等待的车辆
            List<ChargingRequest> waitingRequestsInOtherPiles = new ArrayList<>();
            
            for (ChargingPile otherPile : otherSameTypePiles) {
                // 获取该充电桩中等待的请求（不包括充电中的）
                LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(ChargingRequest::getChargingPileId, otherPile.getId())
                           .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING);
                
                List<ChargingRequest> waitingRequests = chargingRequestMapper.selectList(queryWrapper);
                waitingRequestsInOtherPiles.addAll(waitingRequests);
            }
            
            // 如果有等待的车辆，进行重新调度
            if (!waitingRequestsInOtherPiles.isEmpty()) {
                // 将这些车辆设为优先等待并放回等候区
                for (ChargingRequest request : waitingRequestsInOtherPiles) {
                    // 使用UpdateWrapper来明确更新null值
                    LambdaUpdateWrapper<ChargingRequest> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(ChargingRequest::getId, request.getId())
                                .set(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                                .set(ChargingRequest::getChargingPileId, null)  // 明确设置为null
                                .set(ChargingRequest::getQueuePosition, null);  // 明确设置为null
                    
                    // 更新数据库
                    chargingRequestMapper.update(null, updateWrapper);
                    
                    // 发送重新调度通知给用户
                    sendRebalanceNotification(request, pile);
                }
            }
            
        } catch (Exception e) {
        }
    }
    
    /**
     * 发送重新调度通知给用户
     * 
     * @param request 受影响的充电请求
     * @param recoveredPile 恢复的充电桩
     */
    private void sendRebalanceNotification(ChargingRequest request, ChargingPile recoveredPile) {
        try {
            // 构建通知消息
            PileFailureNotificationDTO notification = new PileFailureNotificationDTO(
                "PILE_RECOVERY_REBALANCE",
                recoveredPile.getPileNumber(),
                request.getRequestId(),
                "充电桩恢复重新调度通知",
                String.format("充电桩 %s 已恢复正常，为了优化整体等待时间，您的充电请求已被重新优先调度。", 
                    recoveredPile.getPileNumber()),
                "INFO"
            );
            
            // 向用户发送通知
            String notificationKey = "notification_user_" + request.getUserId();
            sseManager.sendToEmitter(notificationKey, notification);
            
        } catch (Exception e) {
        }
    }
} 