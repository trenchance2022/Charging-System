package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.exception.ChargingAmountValidationException;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.dto.ChargingRequestDTO;
import com.example.backend.model.dto.ChargingResponseDTO;
import com.example.backend.model.dto.ChargingStatusDTO;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.model.entity.User;
import com.example.backend.service.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChargingRequestServiceImpl extends ServiceImpl<ChargingRequestMapper, ChargingRequest> implements ChargingRequestService {
    
    @Autowired
    private ChargingPileService chargingPileService;
    
    @Autowired
    private ChargingBillService chargingBillService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Autowired
    private ChargingStatusService chargingStatusService;
    
    @Autowired
    private PricingService pricingService;
    
    @Autowired
    private SystemConstantService systemConstantService;
    
    @Override
    @Transactional
    public ChargingResponseDTO submitRequest(Long userId, ChargingRequestDTO requestDTO) {
        // 检查用户是否已设置电池容量
        User user = userService.getUserById(userId);
        if (user == null || user.getBatteryCapacity() == null || user.getBatteryCapacity() <= 0) {
            return new ChargingResponseDTO(null, "请先设置电池容量后再提交充电请求", "ERROR");
        }
        
        // 检查用户是否已有活跃的充电请求
        ChargingRequest existingRequest = getActiveRequest(userId);
        if (existingRequest != null) {
            return new ChargingResponseDTO(existingRequest.getRequestId(), 
                "您已有一个活跃的充电请求（排队号码: " + existingRequest.getRequestId() + "），请先完成或取消当前请求", "ERROR");
        }
        
        // 验证充电量：不能大于 (电池容量 - 当前电量)
        Double batteryCapacity = user.getBatteryCapacity();
        Double currentPower = user.getCurrentPower() != null ? user.getCurrentPower() : 0.0;
        Double requestedAmount = requestDTO.getChargingAmount();
        Double maxAllowedAmount = batteryCapacity - currentPower;
        
        if (requestedAmount == null || requestedAmount <= 0) {
            throw new ChargingAmountValidationException("请求充电量必须大于0");
        }
        
        if (requestedAmount > maxAllowedAmount) {
            throw new ChargingAmountValidationException(
                String.format("请求充电量 %.1f kWh 超过最大可充电量 %.1f kWh (电池容量 %.1f kWh - 当前电量 %.1f kWh)", 
                    requestedAmount, maxAllowedAmount, batteryCapacity, currentPower)
            );
        }
        
        // 检查等候区是否已满（从数据库获取等候区容量）
        String capacityStr = systemConfigService.getConfigValue(ChargingConstants.CONFIG_WAITING_AREA_CAPACITY);
        Integer waitingAreaCapacity = Integer.parseInt(capacityStr);
        long currentWaitingCount = count(new LambdaQueryWrapper<ChargingRequest>()
                .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                .isNull(ChargingRequest::getChargingPileId));
        
        if (currentWaitingCount >= waitingAreaCapacity) {
            return new ChargingResponseDTO(null, "等候区已满，请稍后再试", "ERROR");
        }
        
        // 生成请求ID
        String chargingMode = requestDTO.getChargingMode();
        String requestId = generateRequestId(chargingMode);
        
        // 创建充电请求
        ChargingRequest request = new ChargingRequest();
        request.setRequestId(requestId);
        request.setUserId(userId);
        request.setChargingMode(chargingMode);
        request.setChargingAmount(requestDTO.getChargingAmount());
        request.setStatus(ChargingConstants.STATUS_WAITING);
        request.setCreateTime(LocalDateTime.now());
        
        // 保存请求
        save(request);
        
        // 返回响应
        return new ChargingResponseDTO(requestId, "充电请求已提交，排队号码: " + requestId, ChargingConstants.STATUS_WAITING);
    }
    
    @Override
    @Transactional
    public ChargingResponseDTO startCharging(Long userId) {
        // 查找用户的活跃请求
        ChargingRequest request = getActiveRequest(userId);
        if (request == null) {
            return new ChargingResponseDTO(null, "没有找到活跃的充电请求", "ERROR");
        }
        
        String requestId = request.getRequestId();
        
        // 验证请求状态必须是等待中
        if (!ChargingConstants.STATUS_WAITING.equals(request.getStatus())) {
            return new ChargingResponseDTO(requestId, "请求状态不允许开始充电", "ERROR");
        }
        
        // 验证请求必须已经分配充电桩
        if (request.getChargingPileId() == null) {
            return new ChargingResponseDTO(requestId, "请求尚未分配充电桩，无法开始充电", "ERROR");
        }
        
        // 验证请求必须在充电桩队列的队首位置
        if (request.getQueuePosition() == null || request.getQueuePosition() != 1) {
            return new ChargingResponseDTO(requestId, "您的排队位置为" + request.getQueuePosition() + "，请等待前面的车辆充电完成", "ERROR");
        }
        
        // 验证充电桩状态必须可用
        ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
        if (pile == null) {
            return new ChargingResponseDTO(requestId, "充电桩信息不存在", "ERROR");
        }
        
        if (ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(pile.getStatus())) {
            return new ChargingResponseDTO(requestId, "充电桩当前不可用，无法开始充电", "ERROR");
        }
        
        // 更新请求状态
        request.setStatus(ChargingConstants.STATUS_CHARGING);
        request.setStartTime(LocalDateTime.now());
        updateById(request);
        
        // 更新充电桩状态
        pile.setStatus(ChargingConstants.PILE_STATUS_CHARGING);
        chargingPileService.updateById(pile);
        
        return new ChargingResponseDTO(requestId, "充电已开始", ChargingConstants.STATUS_CHARGING);
    }
    
    @Override
    @Transactional
    public ChargingResponseDTO stopCharging(Long userId) {
        // 查找用户的活跃请求
        ChargingRequest request = getActiveRequest(userId);
        if (request == null) {
            return new ChargingResponseDTO(null, "没有找到活跃的充电请求", "ERROR");
        }
        
        String requestId = request.getRequestId();
        
        // 严格验证：只有充电中状态才能结束充电
        if (!ChargingConstants.STATUS_CHARGING.equals(request.getStatus())) {
            return new ChargingResponseDTO(requestId, "只有充电中的请求才能结束充电", "ERROR");
        }
        
        // 计算实际充电量并更新用户当前电量
        updateUserCurrentPowerAfterCharging(request);
        
        // 更新请求状态
        request.setStatus(ChargingConstants.STATUS_COMPLETED);
        request.setEndTime(LocalDateTime.now());
        updateById(request);
        
        // 更新充电桩状态
        ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
        pile.setStatus(ChargingConstants.PILE_STATUS_AVAILABLE);
        chargingPileService.updateById(pile);
        
        // 生成充电详单
        chargingBillService.generateBill(request);
        
        // 更新同充电桩队列中的其他请求位置
        updateQueuePositions(request.getChargingPileId());
        
        return new ChargingResponseDTO(requestId, "充电已结束", ChargingConstants.STATUS_COMPLETED);
    }
    
    @Override
    @Transactional
    public ChargingResponseDTO autoStopCharging(String requestId) {
        ChargingRequest request = getRequestByRequestId(requestId);
        if (request == null) {
            return new ChargingResponseDTO(requestId, "找不到对应的充电请求", "ERROR");
        }
        
        // 计算实际充电量并更新用户当前电量
        updateUserCurrentPowerAfterCharging(request);
        
        // 更新请求状态
        request.setStatus(ChargingConstants.STATUS_COMPLETED);
        request.setEndTime(LocalDateTime.now());
        updateById(request);
        
        // 更新充电桩状态
        ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
        if (pile != null) {
            pile.setStatus(ChargingConstants.PILE_STATUS_AVAILABLE);
            chargingPileService.updateById(pile);
        }
        
        // 生成充电详单
        chargingBillService.generateBill(request);
        
        // 更新同充电桩队列中的其他请求位置
        updateQueuePositions(request.getChargingPileId());
        
        // 推送充电完成状态到前端
        chargingStatusService.pushChargingStatusUpdate(requestId);
        
        return new ChargingResponseDTO(requestId, "充电已自动结束", ChargingConstants.STATUS_COMPLETED);
    }
    
    @Override
    @Transactional
    public ChargingResponseDTO cancelRequest(Long userId) {
        // 查找用户的活跃请求（等待中或充电中的请求）
        ChargingRequest request = getActiveRequest(userId);
        if (request == null) {
            return new ChargingResponseDTO(null, "没有找到活跃的充电请求", "ERROR");
        }
        
        String requestId = request.getRequestId();
        
        // 验证请求状态：只有WAITING和PRIORITY_WAITING状态才能取消
        if (!ChargingConstants.STATUS_WAITING.equals(request.getStatus()) && 
            !ChargingConstants.STATUS_PRIORITY_WAITING.equals(request.getStatus())) {
            return new ChargingResponseDTO(requestId, "只有等待中的请求才能取消充电", "ERROR");
        }
        
        // 更新请求状态
        request.setStatus(ChargingConstants.STATUS_CANCELED);
        updateById(request);
        
        // 如果请求已分配到充电桩
        if (request.getChargingPileId() != null) {
            // 更新同充电桩队列中的其他请求位置
            updateQueuePositions(request.getChargingPileId());
        }
        
        return new ChargingResponseDTO(requestId, "充电请求已取消", ChargingConstants.STATUS_CANCELED);
    }
    
    @Override
    public ChargingStatusDTO getChargingStatus(String requestId) {
        ChargingRequest request = getRequestByRequestId(requestId);
        if (request == null) {
            return new ChargingStatusDTO("NOT_FOUND", 0.0, 0.0, 0.0, 0.0, 0, false, null, false, null);
        }
        
        // 获取用户信息以获取电池总容量和当前电量
        User user = userService.getUserById(request.getUserId());
        Double totalCapacity = (user != null && user.getBatteryCapacity() != null) ? user.getBatteryCapacity() : 0.0;
        Double currentPower = (user != null && user.getCurrentPower() != null) ? user.getCurrentPower() : 0.0;
        
        String status = request.getStatus();
        Double chargedAmount = 0.0;  // 本次充电请求的已充电量
        Double requestedAmount = request.getChargingAmount();  // 请求充电量
        Integer remainingTime = null;
        Boolean isQueueFirst = false;
        Integer chargingPileId = request.getChargingPileId();
        Boolean isAutoCompleted = false;
        String chargingPileStatus = null;
        
        // 计费相关变量
        BigDecimal currentTotalFee = BigDecimal.ZERO;
        BigDecimal estimatedTotalFee = BigDecimal.ZERO;
        
        // 如果有充电桩ID，获取充电桩状态
        if (chargingPileId != null) {
            ChargingPile pile = chargingPileService.getById(chargingPileId);
            if (pile != null) {
                chargingPileStatus = pile.getStatus();
            }
        }
        
        // 如果已分配充电桩且在等待状态，判断是否在队首
        if (ChargingConstants.STATUS_WAITING.equals(status) && chargingPileId != null) {
            // 检查请求在充电桩队列中的位置
            isQueueFirst = (request.getQueuePosition() != null && request.getQueuePosition() == 1);
            
            // 计算预计费用（基于请求充电量）
            if (requestedAmount != null && requestedAmount > 0) {
                // 根据充电模式获取充电功率
                double chargingPower = getChargingPowerByMode(request.getChargingMode());
                
                // 计算预计充电时长（分钟）- 使用精确计算，避免不必要的舍入
                double exactChargingMinutes = (requestedAmount * 60) / chargingPower;
                long estimatedChargingMinutes = Math.round(exactChargingMinutes);
                
                LocalDateTime estimatedStartTime = LocalDateTime.now();
                LocalDateTime estimatedEndTime = estimatedStartTime.plusMinutes(estimatedChargingMinutes);
                
                BigDecimal chargingFee = chargingBillService.calculateChargingFeeWithTimeBasedPricing(
                    estimatedStartTime, 
                    estimatedEndTime, 
                    requestedAmount
                );
                BigDecimal serviceFee = pricingService.getServiceFeeRate()
                    .multiply(BigDecimal.valueOf(requestedAmount))
                    .setScale(2, RoundingMode.HALF_UP);
                estimatedTotalFee = chargingFee.add(serviceFee);
            }
        }
        
        if (ChargingConstants.STATUS_CHARGING.equals(status)) {
            // 计算本次充电的已充电量和剩余时间
            ChargingPile pile = chargingPileService.getById(request.getChargingPileId());

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = request.getStartTime();
            
            // 计算已充电时间（分钟）
            long chargedMinutes = ChronoUnit.MINUTES.between(startTime, now);
            
            // 计算本次充电的已充电量 = 充电功率 * 充电时间 / 60
            chargedAmount = (pile.getChargingPower() * chargedMinutes) / 60;
            
            // 计算剩余充电时间（分钟）
            double remainingAmount = request.getChargingAmount() - chargedAmount;
            if (remainingAmount > 0) {
                remainingTime = (int) Math.ceil((remainingAmount * 60) / pile.getChargingPower());
            }
            isQueueFirst = true;
            
            // 计算当前已产生的费用
            if (chargedAmount > 0) {
                BigDecimal chargingFee = chargingBillService.calculateChargingFeeWithTimeBasedPricing(
                    startTime, now, chargedAmount);
                BigDecimal serviceFee = pricingService.getServiceFeeRate()
                    .multiply(BigDecimal.valueOf(chargedAmount))
                    .setScale(2, RoundingMode.HALF_UP);
                currentTotalFee = chargingFee.add(serviceFee);
            }
            
            // 计算预计总费用（基于请求充电量）
            if (requestedAmount != null && requestedAmount > 0) {
                // 根据充电模式获取充电功率
                double chargingPower = getChargingPowerByMode(request.getChargingMode());
                
                // 计算预计充电时长（分钟）- 使用精确计算，避免不必要的舍入
                double exactChargingMinutes = (requestedAmount * 60) / chargingPower;
                long estimatedChargingMinutes = Math.round(exactChargingMinutes);
                
                LocalDateTime estimatedEndTime = startTime.plusMinutes(estimatedChargingMinutes);
                
                BigDecimal chargingFee = chargingBillService.calculateChargingFeeWithTimeBasedPricing(
                    startTime, 
                    estimatedEndTime, 
                    requestedAmount
                );
                BigDecimal serviceFee = pricingService.getServiceFeeRate()
                    .multiply(BigDecimal.valueOf(requestedAmount))
                    .setScale(2, RoundingMode.HALF_UP);
                estimatedTotalFee = chargingFee.add(serviceFee);
            }
            
        } else if (ChargingConstants.STATUS_COMPLETED.equals(status)) {
            // 对于已完成的请求，计算实际充电量和费用
            ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
            // 计算充电时长（分钟）
            long chargedMinutes = ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime());
            // 计算实际充电量
            chargedAmount = (pile.getChargingPower() * chargedMinutes) / 60.0;
            remainingTime = 0;
            isAutoCompleted = chargedAmount >= request.getChargingAmount();
            
            // 计算最终费用
            if (chargedAmount > 0) {
                BigDecimal chargingFee = chargingBillService.calculateChargingFeeWithTimeBasedPricing(
                    request.getStartTime(), request.getEndTime(), chargedAmount);
                BigDecimal serviceFee = pricingService.getServiceFeeRate()
                    .multiply(BigDecimal.valueOf(chargedAmount))
                    .setScale(2, RoundingMode.HALF_UP);
                currentTotalFee = chargingFee.add(serviceFee);
                
                // 已完成状态下，预计费用等于实际费用
                estimatedTotalFee = currentTotalFee;
            }
        }
        
        return new ChargingStatusDTO(
                status,
                currentPower + chargedAmount,  // 实时当前电量 = 原始电量 + 已充电量
                chargedAmount,  // 本次充电的已充电量
                totalCapacity,  // 用户的电池总容量
                requestedAmount,  // 请求充电量
                remainingTime,
                isQueueFirst,
                chargingPileId,
                isAutoCompleted,
                chargingPileStatus,
                currentTotalFee,
                estimatedTotalFee
        );
    }
    
    @Override
    public ChargingRequest getRequestByRequestId(String requestId) {
        return getOne(new LambdaQueryWrapper<ChargingRequest>()
                .eq(ChargingRequest::getRequestId, requestId));
    }
    
    /**
     * 获取请求前面的排队数量
     * 
     * @param requestId 请求ID
     * @return 前面排队的数量
     */
    @Override
    public int getQueueCountBefore(String requestId) {
        ChargingRequest request = getRequestByRequestId(requestId);
        if (request == null || !ChargingConstants.STATUS_WAITING.equals(request.getStatus())) {
            return 0;
        }
        
        // 如果已经分配了充电桩，返回队列位置-1（前面的车辆数）
        if (request.getChargingPileId() != null && request.getQueuePosition() != null) {
            return request.getQueuePosition() - 1;
        }
        
        // 如果还未分配充电桩，计算在等候区中的位置
        // 找出所有等待中且创建时间早于当前请求的请求数量
        long countValue = count(new LambdaQueryWrapper<ChargingRequest>()
                .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                .lt(ChargingRequest::getCreateTime, request.getCreateTime()));
        
        return (int) countValue;
    }
    
    /**
     * 更新充电桩队列中的请求位置
     */
    private void updateQueuePositions(Integer pileId) {
        if (pileId == null) {
            return; // 如果充电桩ID为null，直接返回
        }
        
        // 获取该充电桩的所有等待中请求
        List<ChargingRequest> queueRequests = list(new LambdaQueryWrapper<ChargingRequest>()
                .eq(ChargingRequest::getChargingPileId, pileId)
                .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                .orderBy(true, true, ChargingRequest::getQueuePosition));
        
        // 重新设置队列位置
        for (int i = 0; i < queueRequests.size(); i++) {
            ChargingRequest request = queueRequests.get(i);
            request.setQueuePosition(i + 1);
            updateById(request);
        }
    }
    
    /**
     * 生成请求ID
     * 调用mapper层的复杂查询，简化service层逻辑
     */
    @Override
    public String generateRequestId(String chargingMode) {
        String prefix = ChargingConstants.MODE_FAST.equals(chargingMode) ? 
                ChargingConstants.REQUEST_PREFIX_FAST : ChargingConstants.REQUEST_PREFIX_SLOW;
        
        try {
            // 调用mapper层查询最大序列号
            Integer maxSeq = getBaseMapper().selectMaxSequenceByModeAndPrefix(
                chargingMode, prefix, prefix.length() + 1);
            
            // 如果没有找到记录，从1开始
            int nextSeq = (maxSeq != null) ? maxSeq + 1 : 1;
            
            return prefix + nextSeq;
            
        } catch (Exception e) {
            // 如果查询失败，回退到简单查询
            return generateRequestIdFallback(chargingMode, prefix);
        }
    }
    
    /**
     * 后备方案：简单的ID生成方法
     * 当复杂查询失败时使用
     */
    private String generateRequestIdFallback(String chargingMode, String prefix) {
        // 查询所有该模式的请求ID，仅选择ID字段
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingMode, chargingMode)
                   .likeRight(ChargingRequest::getRequestId, prefix)
                   .select(ChargingRequest::getRequestId);
        
        List<ChargingRequest> requests = list(queryWrapper);
        
        int maxSeq = 0;
        for (ChargingRequest request : requests) {
            if (request.getRequestId() != null && request.getRequestId().startsWith(prefix)) {
                String numPart = request.getRequestId().substring(prefix.length());
                int seq = Integer.parseInt(numPart);
                maxSeq = Math.max(maxSeq, seq);
            }
        }
        
        return prefix + (maxSeq + 1);
    }
    
    @Override
    @Transactional
    public ChargingResponseDTO modifyActiveRequest(Long userId, ChargingRequestDTO requestDTO) {
        // 检查用户是否已设置电池容量
        User user = userService.getUserById(userId);
        if (user == null || user.getBatteryCapacity() == null || user.getBatteryCapacity() <= 0) {
            return new ChargingResponseDTO(null, "请先设置电池容量后再修改充电请求", "ERROR");
        }
        
        // 验证充电量：不能大于 (电池容量 - 当前电量)
        Double batteryCapacity = user.getBatteryCapacity();
        Double currentPower = user.getCurrentPower() != null ? user.getCurrentPower() : 0.0;
        Double requestedAmount = requestDTO.getChargingAmount();
        Double maxAllowedAmount = batteryCapacity - currentPower;
        
        if (requestedAmount == null || requestedAmount <= 0) {
            throw new ChargingAmountValidationException("请求充电量必须大于0");
        }
        
        if (requestedAmount > maxAllowedAmount) {
            throw new ChargingAmountValidationException(
                String.format("请求充电量 %.1f kWh 超过最大可充电量 %.1f kWh (电池容量 %.1f kWh - 当前电量 %.1f kWh)", 
                    requestedAmount, maxAllowedAmount, batteryCapacity, currentPower)
            );
        }
        
        // 查找用户的活跃请求（等待中或充电中的请求）
        ChargingRequest request = getActiveRequest(userId);
        if (request == null) {
            return new ChargingResponseDTO(null, "没有找到活跃的充电请求", "ERROR");
        }
        
        // 获取请求ID，便于后续处理
        String requestId = request.getRequestId();
        
        // 只有WAITING状态的请求才能修改
        if (!ChargingConstants.STATUS_WAITING.equals(request.getStatus())) {
            return new ChargingResponseDTO(requestId, "只有等待中的充电请求才能修改", "ERROR");
        }
        
        boolean modeChanged = !request.getChargingMode().equals(requestDTO.getChargingMode());
        Double originalAmount = request.getChargingAmount();
        
        // 如果请求尚未分配充电桩（在等候区），则可以修改模式和充电量
        if (request.getChargingPileId() == null) {
            // 如果充电模式变更，则需要重新生成请求ID并重新排队
            if (modeChanged) {
                // 取消原来的请求
                request.setStatus(ChargingConstants.STATUS_CANCELED);
                updateById(request);
                
                // 生成新的请求ID
                String newRequestId = generateRequestId(requestDTO.getChargingMode());
                
                // 创建新的请求
                ChargingRequest newRequest = new ChargingRequest();
                newRequest.setRequestId(newRequestId);
                newRequest.setUserId(userId);
                newRequest.setChargingMode(requestDTO.getChargingMode());
                newRequest.setChargingAmount(requestDTO.getChargingAmount());
                newRequest.setStatus(ChargingConstants.STATUS_WAITING);
                newRequest.setCreateTime(LocalDateTime.now());
                
                // 保存新请求
                save(newRequest);
                
                return new ChargingResponseDTO(newRequestId, "充电模式已修改，新的排队号码为: " + newRequestId, ChargingConstants.STATUS_WAITING);
            } else {
                // 仅修改充电量，不变更排队号
                request.setChargingAmount(requestDTO.getChargingAmount());
                updateById(request);
                
                return new ChargingResponseDTO(requestId, "充电量已修改", ChargingConstants.STATUS_WAITING);
            }
        }
        // 请求已在充电区排队
        else {
            // 在充电区不允许修改充电模式
            if (modeChanged) {
                return new ChargingResponseDTO(requestId, "已进入充电区的请求不能修改充电模式，如需修改请取消后重新提交", "ERROR");
            }
            
            // 在充电区不允许修改充电量
            if (!originalAmount.equals(requestDTO.getChargingAmount())) {
                return new ChargingResponseDTO(requestId, "已进入充电区的请求不能修改充电量，如需修改请取消后重新提交", "ERROR");
            }
            
            // 没有任何修改
            return new ChargingResponseDTO(requestId, "请求未发生变化", ChargingConstants.STATUS_WAITING);
        }
    }
    
    /**
     * 获取用户活跃的充电请求
     * @param userId 用户ID
     * @return 活跃的充电请求，如果没有则返回null
     */
    public ChargingRequest getActiveRequest(Long userId) {
        // 查询用户的WAITING、PRIORITY_WAITING或CHARGING状态的请求
        List<ChargingRequest> activeRequests = list(new LambdaQueryWrapper<ChargingRequest>()
                .eq(ChargingRequest::getUserId, userId)
                .in(ChargingRequest::getStatus, 
                    ChargingConstants.STATUS_WAITING, 
                    ChargingConstants.STATUS_PRIORITY_WAITING,
                    ChargingConstants.STATUS_CHARGING));
        
        // 一个用户应该只有一个活跃请求
        return activeRequests.isEmpty() ? null : activeRequests.get(0);
    }
    
    @Override
    public ChargingStatusDTO getActiveChargingStatus(Long userId) {
        // 查找用户的活跃请求
        ChargingRequest request = getActiveRequest(userId);
        
        if (request == null) {
            // 即使没有活跃请求，也要返回用户的基本信息
            User user = userService.getUserById(userId);
            Double totalCapacity = (user != null && user.getBatteryCapacity() != null) ? user.getBatteryCapacity() : 0.0;
            Double currentPower = (user != null && user.getCurrentPower() != null) ? user.getCurrentPower() : 0.0;
            
            return new ChargingStatusDTO(
                "NOT_FOUND", 
                currentPower,  // 当前电量
                0.0,          // 已充电量（无活跃请求时为0）
                totalCapacity, // 电池总容量
                0.0,          // 请求充电量（无活跃请求时为0）
                0,            // 剩余时间
                false,        // 不在队首
                null,         // 没有充电桩ID
                false,        // 不是自动完成
                null          // 没有充电桩状态
            );
        }
        
        // 使用已有方法获取状态
        return getChargingStatus(request.getRequestId());
    }
    
    @Override
    public void updateUserCurrentPowerAfterCharging(ChargingRequest request) {
        // 计算实际充电量
        ChargingPile pile = chargingPileService.getById(request.getChargingPileId());
        if (pile != null && request.getStartTime() != null) {
            LocalDateTime now = LocalDateTime.now();
            long chargedMinutes = ChronoUnit.MINUTES.between(request.getStartTime(), now);
            Double actualChargedAmount = (pile.getChargingPower() * chargedMinutes) / 60.0;
            
            // 不能超过请求的充电量
            actualChargedAmount = Math.min(actualChargedAmount, request.getChargingAmount());
            
            // 获取用户信息
            User user = userService.getUserById(request.getUserId());
            
            if (user != null) {
                // 获取用户当前电量
                Double currentPower = user.getCurrentPower() != null ? user.getCurrentPower() : 0.0;
                
                // 计算实际充电后的当前电量
                Double newCurrentPower = currentPower + actualChargedAmount;
                
                // 更新用户当前电量
                userService.updateCurrentPower(request.getUserId(), newCurrentPower);
            }
        }
    }
    
    @Override
    public List<Map<String, Object>> getPileQueueInfo(Integer pileId) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .in(ChargingRequest::getStatus, ChargingConstants.STATUS_CHARGING, ChargingConstants.STATUS_WAITING)
                   .orderBy(true, true, ChargingRequest::getQueuePosition);
        
        List<ChargingRequest> queueRequests = list(queryWrapper);
        
        List<Map<String, Object>> queueInfo = new ArrayList<>();
        
        for (ChargingRequest request : queueRequests) {
            Map<String, Object> info = new HashMap<>();
            
            // 获取用户信息
            User user = userService.getUserById(request.getUserId());
            if (user != null) {
                info.put("username", user.getUsername());
                info.put("totalCapacity", user.getBatteryCapacity() != null ? user.getBatteryCapacity() : 0.0);
            } else {
                info.put("username", "未知用户");
                info.put("totalCapacity", 0.0);
            }
            
            info.put("chargingAmount", request.getChargingAmount());
            info.put("status", request.getStatus()); // 添加状态信息
            info.put("queuePosition", request.getQueuePosition()); // 添加队列位置
            
            // 计算时长（分钟）
            if (ChargingConstants.STATUS_CHARGING.equals(request.getStatus())) {
                // 正在充电：计算已充电时长
                if (request.getStartTime() != null) {
                    long chargingTime = ChronoUnit.MINUTES.between(request.getStartTime(), LocalDateTime.now());
                    info.put("timeInfo", chargingTime);
                    info.put("timeLabel", "已充电时长");
                } else {
                    info.put("timeInfo", 0);
                    info.put("timeLabel", "已充电时长");
                }
            } else {
                // 正在等待：计算排队时长
                long waitTime = ChronoUnit.MINUTES.between(request.getCreateTime(), LocalDateTime.now());
                info.put("timeInfo", waitTime);
                info.put("timeLabel", "排队时长");
            }
            
            queueInfo.add(info);
        }
        
        return queueInfo;
    }
    
    /**
     * 根据充电模式获取充电功率
     * @param chargingMode 充电模式（fast或slow）
     * @return 充电功率（kW）
     */
    private double getChargingPowerByMode(String chargingMode) {
        try {
            if (ChargingConstants.MODE_FAST.equals(chargingMode)) {
                Object powerObj = systemConstantService.getConstant(ChargingConstants.CONFIG_FAST_CHARGING_POWER);
                if (powerObj instanceof Double) {
                    return (Double) powerObj;
                }
            } else if (ChargingConstants.MODE_SLOW.equals(chargingMode)) {
                Object powerObj = systemConstantService.getConstant(ChargingConstants.CONFIG_SLOW_CHARGING_POWER);
                if (powerObj instanceof Double) {
                    return (Double) powerObj;
                }
            }
        } catch (Exception e) {
            // 如果获取配置失败，使用默认值
        }
        
        // 默认值：快充30kW，慢充7kW
        return ChargingConstants.MODE_FAST.equals(chargingMode) ? 30.0 : 7.0;
    }
}
