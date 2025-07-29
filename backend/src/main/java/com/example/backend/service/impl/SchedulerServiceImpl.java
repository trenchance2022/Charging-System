package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingPileService;
import com.example.backend.service.SchedulerService;
import com.example.backend.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
public class SchedulerServiceImpl implements SchedulerService {
    @Autowired
    private ChargingPileService chargingPileService;
    
    @Autowired
    private ChargingRequestMapper chargingRequestMapper;
    
    @Autowired
    private SystemConfigService systemConfigService;
    
    @Override
    public ChargingPile findOptimalPile(ChargingRequest request) {
        String chargingMode = request.getChargingMode();
        Double chargingAmount = request.getChargingAmount();
        
        // 根据充电模式确定充电桩类型
        String pileType = ChargingConstants.MODE_FAST.equals(chargingMode) ? 
                ChargingConstants.PILE_TYPE_FAST : ChargingConstants.PILE_TYPE_SLOW;
        
        // 获取对应类型的可用充电桩
        List<ChargingPile> availablePiles = chargingPileService.getAvailablePilesByType(pileType);
        
        if (availablePiles.isEmpty()) {
            return null; // 没有可用的充电桩
        }
        
        // 过滤掉队列已满的充电桩
        List<ChargingPile> availablePilesWithCapacity = new ArrayList<>();
        int maxQueueLength = getMaxQueueLengthFromConfig();
        
        for (ChargingPile pile : availablePiles) {
            if (hasAvailableCapacity(pile, maxQueueLength)) {
                availablePilesWithCapacity.add(pile);
            }
        }
        
        if (availablePilesWithCapacity.isEmpty()) {
            return null; // 所有充电桩队列都已满
        }
        
        ChargingPile optimalPile = null;
        int minTotalTime = Integer.MAX_VALUE;
        
        // 遍历有剩余容量的充电桩，找到总时长最短的
        for (ChargingPile pile : availablePilesWithCapacity) {
            int totalTime = calculateTotalTime(pile, chargingAmount);
            if (totalTime < minTotalTime) {
                minTotalTime = totalTime;
                optimalPile = pile;
            }
        }
        
        return optimalPile;
    }
    
    @Override
    public int calculateTotalTime(ChargingPile pile, Double chargingAmount) {
        int waitingTime = calculateWaitingTime(pile);
        int chargingTime = calculateChargingTime(chargingAmount, pile.getChargingPower());
        return waitingTime + chargingTime;
    }
    
    @Override
    public int calculateWaitingTime(ChargingPile pile) {
        int totalWaitingTime = 0;
        
        // 如果充电桩正在充电中，计算当前充电的剩余时间
        if (ChargingConstants.PILE_STATUS_CHARGING.equals(pile.getStatus())) {
            ChargingRequest currentRequest = getCurrentChargingRequest(pile.getId());
            if (currentRequest != null) {
                totalWaitingTime += calculateRemainingChargingTime(currentRequest, pile);
            }
        }
        
        // 计算队列中等待的请求的充电时间
        List<ChargingRequest> queueRequests = getQueueRequests(pile.getId());
        for (ChargingRequest request : queueRequests) {
            totalWaitingTime += calculateChargingTime(request.getChargingAmount(), pile.getChargingPower());
        }
        
        return totalWaitingTime;
    }
    
    @Override
    public int calculateChargingTime(Double chargingAmount, Double chargingPower) {
        if (chargingAmount == null || chargingPower == null || chargingPower <= 0) {
            return 0;
        }
        // 充电时间（分钟） = 充电量（kWh） / 充电功率（kW） * 60
        return (int) Math.ceil((chargingAmount / chargingPower) * 60);
    }
    
    @Override
    @Scheduled(fixedRate = 10000) // 每10秒执行一次调度
    public void processWaitingRequests() {
        // 首先检查是否有优先级等待请求
        if (hasPriorityWaitingRequests()) {
            // 如果有优先级请求，使用ORIGINAL策略专门处理这些请求，暂停普通请求调度
            processPriorityWaitingRequests();
            return; // 暂停处理普通WAITING请求
        }
        
        // 如果没有优先级请求，按配置策略处理普通等待请求
        String scheduleStrategy = systemConfigService.getConfigValue(ChargingConstants.CONFIG_SCHEDULE_STRATEGY);
        
        // 根据配置的调度策略执行不同的调度算法
        switch (scheduleStrategy) {
            case ChargingConstants.SCHEDULE_STRATEGY_ORIGINAL:
                processWaitingRequestsOriginal();
                break;
            case ChargingConstants.SCHEDULE_STRATEGY_SINGLE_BATCH_OPTIMAL:
                processWaitingRequestsSingleBatchOptimal();
                break;
            case ChargingConstants.SCHEDULE_STRATEGY_FULL_BATCH_OPTIMAL:
                processWaitingRequestsFullBatchOptimal();
                break;
            default:
                processWaitingRequestsOriginal();
                break;
        }
        
    }
    
    /**
     * 检查是否有优先级等待请求
     * @return 如果存在PRIORITY_WAITING状态的请求返回true，否则返回false
     */
    private boolean hasPriorityWaitingRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                   .isNull(ChargingRequest::getChargingPileId);
        
        Long count = chargingRequestMapper.selectCount(queryWrapper);
        return count > 0;
    }
    
    /**
     * 处理优先级等待请求
     * 使用ORIGINAL调度策略专门为PRIORITY_WAITING请求进行调度
     */
    private void processPriorityWaitingRequests() {
        // 获取等候区中未分配充电桩的优先级请求
        List<ChargingRequest> priorityRequests = getPriorityWaitingRequests();
        
        for (ChargingRequest request : priorityRequests) {
            ChargingPile optimalPile = findOptimalPile(request);
            if (optimalPile != null) {
                // 先将优先等待状态改回普通等待状态
                request.setStatus(ChargingConstants.STATUS_WAITING);
                chargingRequestMapper.updateById(request);
                
                // 然后分配到充电桩队列
                assignRequestToPile(request, optimalPile);
            }
        }
    }
    
    /**
     * 获取等候区中未分配充电桩的优先级请求
     * @return 优先级等待请求列表
     */
    private List<ChargingRequest> getPriorityWaitingRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_PRIORITY_WAITING)
                   .isNull(ChargingRequest::getChargingPileId)
                   .orderBy(true, true, ChargingRequest::getCreateTime);
        
        return chargingRequestMapper.selectList(queryWrapper);
    }
    
    /**
     * 原始调度算法 - 现有的实现
     */
    private void processWaitingRequestsOriginal() {
        // 获取等候区中未分配充电桩的请求
        List<ChargingRequest> waitingRequests = getWaitingRequests();
        
        for (ChargingRequest request : waitingRequests) {
            ChargingPile optimalPile = findOptimalPile(request);
            if (optimalPile != null) {
                assignRequestToPile(request, optimalPile);
            }
        }
    }
    
    /**
     * 单批次最优调度算法
     * 当充电区某种模式的充电桩出现根据参数指定的空位数N时，
     * 系统要在等候区该模式对应的队列中，按照编号顺序一次性叫N个车，
     * 此时进入充电区的多辆车不再按照编号顺序依次调度，而是"统一调度"
     */
    private void processWaitingRequestsSingleBatchOptimal() {
        // 获取批次调度阈值配置
        int fastBatchThreshold = getFastBatchThreshold();
        int slowBatchThreshold = getSlowBatchThreshold();
        
        // 检查快充桩空位数量并处理快充批次调度
        int fastAvailableSlots = getAvailableSlotsByMode(ChargingConstants.MODE_FAST);
        if (fastAvailableSlots >= fastBatchThreshold) {
            processSingleBatchForMode(ChargingConstants.MODE_FAST, fastBatchThreshold);
        }
        
        // 检查慢充桩空位数量并处理慢充批次调度
        int slowAvailableSlots = getAvailableSlotsByMode(ChargingConstants.MODE_SLOW);
        if (slowAvailableSlots >= slowBatchThreshold) {
            processSingleBatchForMode(ChargingConstants.MODE_SLOW, slowBatchThreshold);
        }
    }
    
    /**
     * 获取快充批次调度阈值
     */
    private int getFastBatchThreshold() {
        try {
            String thresholdStr = systemConfigService.getConfigValue(ChargingConstants.CONFIG_FAST_BATCH_THRESHOLD);
            if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
                return Integer.parseInt(thresholdStr.trim());
            }
        } catch (Exception e) {
            // 配置获取失败，使用默认值
        }
        return 2; // 默认快充批次阈值
    }
    
    /**
     * 获取慢充批次调度阈值
     */
    private int getSlowBatchThreshold() {
        try {
            String thresholdStr = systemConfigService.getConfigValue(ChargingConstants.CONFIG_SLOW_BATCH_THRESHOLD);
            if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
                return Integer.parseInt(thresholdStr.trim());
            }
        } catch (Exception e) {
            // 配置获取失败，使用默认值
        }
        return 3; // 默认慢充批次阈值
    }
    
    /**
     * 获取指定充电模式的可用空位数量
     */
    private int getAvailableSlotsByMode(String chargingMode) {
        // 根据充电模式确定充电桩类型
        String pileType = ChargingConstants.MODE_FAST.equals(chargingMode) ? 
                ChargingConstants.PILE_TYPE_FAST : ChargingConstants.PILE_TYPE_SLOW;
        
        // 获取对应类型的可用充电桩
        List<ChargingPile> piles = chargingPileService.getAvailablePilesByType(pileType);
        
        int totalAvailableSlots = 0;
        for (ChargingPile pile : piles) {
            totalAvailableSlots += getMaxCapacityForPile(pile);
        }
        
        return totalAvailableSlots;
    }
    
    /**
     * 处理指定模式的单批次调度
     */
    private void processSingleBatchForMode(String chargingMode, int batchSize) {
        // 从等候区获取指定模式的车辆，按编号顺序
        List<ChargingRequest> modeRequests = getWaitingRequestsByMode(chargingMode, batchSize);
        
        if (modeRequests.size() < batchSize) {
            return; // 等候区该模式车辆不足批次大小
        }
        
        // 获取该模式对应的可用充电桩
        String pileType = ChargingConstants.MODE_FAST.equals(chargingMode) ? 
                ChargingConstants.PILE_TYPE_FAST : ChargingConstants.PILE_TYPE_SLOW;
        List<ChargingPile> availablePiles = getAvailablePilesByType(pileType);
        
        if (availablePiles.isEmpty()) {
            return; // 没有可用充电桩
        }
        
        // 使用动态规划执行批次优化调度
        executeSingleBatchOptimalScheduling(modeRequests, availablePiles);
    }
    
    /**
     * 获取等候区中指定模式的等待请求（按创建时间排序）
     */
    private List<ChargingRequest> getWaitingRequestsByMode(String chargingMode, int limit) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                   .isNull(ChargingRequest::getChargingPileId)
                   .eq(ChargingRequest::getChargingMode, chargingMode)
                   .orderBy(true, true, ChargingRequest::getCreateTime)
                   .last("LIMIT " + limit);
        
        return chargingRequestMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取指定类型的有空位的充电桩
     */
    private List<ChargingPile> getAvailablePilesByType(String pileType) {
        List<ChargingPile> piles = chargingPileService.getAvailablePilesByType(pileType);
        return piles.stream()
                .filter(pile -> getMaxCapacityForPile(pile) > 0)
                .collect(Collectors.toList());
    }
    
    /**
     * 执行单批次优化调度
     * 使用动态规划算法分配车辆到充电桩，目标是最小化总时长
     */
    private void executeSingleBatchOptimalScheduling(List<ChargingRequest> batchRequests, List<ChargingPile> availablePiles) {
        int n = batchRequests.size(); // 车辆数量
        int m = availablePiles.size(); // 充电桩数量
        
        // 预计算每个充电桩的当前等待时间和容量
        double[] pileCurrentWaitTimes = new double[m];
        int[] pileCapacities = new int[m];
        
        for (int i = 0; i < m; i++) {
            pileCurrentWaitTimes[i] = calculateWaitingTime(availablePiles.get(i));
            pileCapacities[i] = getMaxCapacityForPile(availablePiles.get(i));
        }
        
        // 预计算车辆充电时间矩阵
        double[][] vehicleChargingTimes = new double[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                vehicleChargingTimes[i][j] = calculateChargingTime(
                    batchRequests.get(i).getChargingAmount(),
                    availablePiles.get(j).getChargingPower()
                );
            }
        }
        
        // 使用动态规划求解最优分配
        SingleBatchOptimalAssignment optimalAssignment = solveSingleBatchDP(
            n, m, pileCurrentWaitTimes, pileCapacities, vehicleChargingTimes);
        
        // 执行最优分配
        if (optimalAssignment != null && optimalAssignment.isValid()) {
            // 按充电桩分组，并按充电时间排序分配
            Map<Integer, List<Integer>> pileToVehicles = new HashMap<>();
            
            // 收集每个充电桩分配到的车辆
            for (int i = 0; i < n; i++) {
                if (optimalAssignment.assignments[i] >= 0) {
                    int pileIndex = optimalAssignment.assignments[i];
                    pileToVehicles.computeIfAbsent(pileIndex, k -> new ArrayList<>()).add(i);
                }
            }
            
            // 对每个充电桩的车辆按充电时间排序后分配
            for (Map.Entry<Integer, List<Integer>> entry : pileToVehicles.entrySet()) {
                int pileIndex = entry.getKey();
                List<Integer> vehicles = entry.getValue();
                ChargingPile pile = availablePiles.get(pileIndex);
                
                // 按充电时间从短到长排序
                vehicles.sort((v1, v2) -> Double.compare(
                    vehicleChargingTimes[v1][pileIndex], vehicleChargingTimes[v2][pileIndex]));
                
                // 按排序后的顺序分配到充电桩
                for (int vehicleIndex : vehicles) {
                    assignRequestToPile(batchRequests.get(vehicleIndex), pile);
                }
            }
        }
    }
    
    /**
     * 使用动态规划求解单批次最优分配
     * 状态定义：dp[mask] = 已分配车辆集合为mask时的最小总时长
     */
    private SingleBatchOptimalAssignment solveSingleBatchDP(int n, int m, 
            double[] pileCurrentWaitTimes, int[] pileCapacities, double[][] vehicleChargingTimes) {
        
        // DP状态：dp[mask] 表示已分配车辆集合为mask时的最优状态
        Map<Integer, SingleBatchDPState> dp = new HashMap<>();
        
        // 初始状态：没有车辆被分配
        dp.put(0, new SingleBatchDPState(0, 0, new int[n], new int[m]));
        
        // 逐步扩展状态空间
        for (int mask = 0; mask < (1 << n); mask++) {
            if (!dp.containsKey(mask)) {
                continue;
            }
            
            SingleBatchDPState currentState = dp.get(mask);
            
            // 尝试为每个未分配的车辆分配充电桩
            for (int vehicle = 0; vehicle < n; vehicle++) {
                if ((mask & (1 << vehicle)) != 0) {
                    continue; // 车辆已分配
                }
                
                // 尝试分配到每个有容量的充电桩
                for (int pile = 0; pile < m; pile++) {
                    if (currentState.pileAssignments[pile] >= pileCapacities[pile]) {
                        continue; // 充电桩已满
                    }
                    
                    // 计算分配该车辆到该充电桩后的新状态
                    int newMask = mask | (1 << vehicle);
                    
                    // 创建新的分配状态
                    int[] newVehicleAssignments = currentState.vehicleAssignments.clone();
                    int[] newPileAssignments = currentState.pileAssignments.clone();
                    
                    newVehicleAssignments[vehicle] = pile;
                    newPileAssignments[pile]++;
                    
                    // 计算新状态的累计等待时间+累计充电时间总和
                    double newTotalTime = calculateTotalWaitingAndChargingTime(newVehicleAssignments, 
                        pileCurrentWaitTimes, vehicleChargingTimes);
                    
                    // 计算新状态的makespan
                    double newMakespan = calculateMakespan(newVehicleAssignments, 
                        pileCurrentWaitTimes, vehicleChargingTimes);
                    
                    // 更新新状态（二级优化：主要目标totalTime，次要目标makespan）
                    if (!dp.containsKey(newMask)) {
                        dp.put(newMask, new SingleBatchDPState(newTotalTime, newMakespan, newVehicleAssignments, newPileAssignments));
                    } else {
                        SingleBatchDPState existingState = dp.get(newMask);
                        // 优先保证总时间最优，其次考虑makespan
                        if (newTotalTime < existingState.totalTime || 
                            (Math.abs(newTotalTime - existingState.totalTime) < 1e-9 && newMakespan < existingState.makespan)) {
                            dp.put(newMask, new SingleBatchDPState(newTotalTime, newMakespan, newVehicleAssignments, newPileAssignments));
                        }
                    }
                }
            }
        }
        
        // 查找完全分配状态的最优解
        int fullMask = (1 << n) - 1;
        SingleBatchDPState finalState = dp.get(fullMask);
        
        if (finalState != null) {
            return new SingleBatchOptimalAssignment(finalState.vehicleAssignments, finalState.totalTime, finalState.makespan);
        }
        
        return null;
    }
    
    /**
     * 计算所有车辆的累计等待时间+累计充电时间总和
     * 这是单批次调度的正确优化目标
     */
    private double calculateTotalWaitingAndChargingTime(int[] vehicleAssignments, 
            double[] pileCurrentWaitTimes, double[][] vehicleChargingTimes) {
        
        int n = vehicleAssignments.length;
        int m = pileCurrentWaitTimes.length;
        
        // 计算每个充电桩的服务队列
        double[] pileServiceTimes = pileCurrentWaitTimes.clone();
        double totalWaitingAndChargingTime = 0;
        
        // 按充电桩分组并计算每辆车的等待时间和充电时间
        for (int pile = 0; pile < m; pile++) {
            // 收集分配到该充电桩的车辆
            List<Integer> vehiclesOnPile = new ArrayList<>();
            for (int vehicle = 0; vehicle < n; vehicle++) {
                if (vehicleAssignments[vehicle] == pile) {
                    vehiclesOnPile.add(vehicle);
                }
            }
            
            // 按充电时间从短到长排序（最短处理时间优先），以最小化总等待时间
            final int currentPile = pile;
            vehiclesOnPile.sort((v1, v2) -> Double.compare(
                vehicleChargingTimes[v1][currentPile], vehicleChargingTimes[v2][currentPile]));
            
            // 计算该充电桩上每辆车的等待时间和充电时间
            for (int vehicle : vehiclesOnPile) {
                // 该车的等待时间 = 轮到该车时充电桩的服务时间
                double waitingTime = pileServiceTimes[pile];
                
                // 该车的充电时间
                double chargingTime = vehicleChargingTimes[vehicle][pile];
                
                // 累加等待时间和充电时间
                totalWaitingAndChargingTime += waitingTime + chargingTime;
                
                // 更新充电桩的服务时间（为下一辆车准备）
                pileServiceTimes[pile] += chargingTime;
            }
        }
        
        return totalWaitingAndChargingTime;
    }
    
    /**
     * 计算makespan
     */
    private double calculateMakespan(int[] vehicleAssignments, 
            double[] pileCurrentWaitTimes, double[][] vehicleChargingTimes) {
        
        int n = vehicleAssignments.length;
        int m = pileCurrentWaitTimes.length;
        
        // 计算每个充电桩的完成时间
        double[] pileFinishTimes = pileCurrentWaitTimes.clone();
        
        // 按充电桩分组并计算完成时间
        for (int pile = 0; pile < m; pile++) {
            // 收集分配到该充电桩的车辆
            List<Integer> vehiclesOnPile = new ArrayList<>();
            for (int vehicle = 0; vehicle < n; vehicle++) {
                if (vehicleAssignments[vehicle] == pile) {
                    vehiclesOnPile.add(vehicle);
                }
            }
            
            // 按充电时间从短到长排序（最短处理时间优先）
            final int currentPile = pile;
            vehiclesOnPile.sort((v1, v2) -> Double.compare(
                vehicleChargingTimes[v1][currentPile], vehicleChargingTimes[v2][currentPile]));
            
            // 计算该充电桩的最终完成时间
            for (int vehicle : vehiclesOnPile) {
                double chargingTime = vehicleChargingTimes[vehicle][pile];
                pileFinishTimes[pile] += chargingTime;
            }
        }
        
        // makespan是所有充电桩完成时间的最大值
        double makespan = 0;
        for (double finishTime : pileFinishTimes) {
            if (finishTime > makespan) {
                makespan = finishTime;
            }
        }
        
        return makespan;
    }
    
    /**
     * 单批次动态规划状态类
     */
    private static class SingleBatchDPState {
        double totalTime;             // 当前状态的总时长
        double makespan;              // 当前状态的makespan（最后一辆车完成时间）
        int[] vehicleAssignments;     // 车辆分配：vehicleAssignments[i] = 车辆i分配到的充电桩索引（-1表示未分配）
        int[] pileAssignments;        // 充电桩分配计数：pileAssignments[j] = 充电桩j已分配的车辆数量
        
        public SingleBatchDPState(double totalTime, double makespan, int[] vehicleAssignments, int[] pileAssignments) {
            this.totalTime = totalTime;
            this.makespan = makespan;
            this.vehicleAssignments = vehicleAssignments.clone();
            this.pileAssignments = pileAssignments.clone();
            
            // 初始化车辆分配为-1（未分配）
            for (int i = 0; i < this.vehicleAssignments.length; i++) {
                if (this.vehicleAssignments[i] == 0 && totalTime == 0) {
                    this.vehicleAssignments[i] = -1;
                }
            }
        }
    }
    
    /**
     * 单批次最优分配结果类
     */
    private static class SingleBatchOptimalAssignment {
        int[] assignments;    // 每个车辆对应的充电桩索引
        double totalTime;     // 总时长
        
        public SingleBatchOptimalAssignment(int[] assignments, double totalTime, double makespan) {
            this.assignments = assignments.clone();
            this.totalTime = totalTime;
        }
        
        public boolean isValid() {
            return assignments != null && totalTime < Double.MAX_VALUE;
        }
    }
    
    /**
     * 获取当前在指定充电桩充电的请求
     */
    private ChargingRequest getCurrentChargingRequest(Integer pileId) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_CHARGING);
        
        List<ChargingRequest> chargingRequests = chargingRequestMapper.selectList(queryWrapper);
        
        return chargingRequests.isEmpty() ? null : chargingRequests.get(0);
    }
    
    /**
     * 计算当前充电请求的剩余充电时间
     */
    private int calculateRemainingChargingTime(ChargingRequest request, ChargingPile pile) {
        LocalDateTime startTime = request.getStartTime();
        if (startTime == null) {
            return calculateChargingTime(request.getChargingAmount(), pile.getChargingPower());
        }
        
        // 计算已充电时间（分钟）
        long chargedMinutes = ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
        
        // 计算已充电量
        double chargedAmount = (pile.getChargingPower() * chargedMinutes) / 60;
        
        // 计算剩余充电量
        double remainingAmount = request.getChargingAmount() - chargedAmount;
        
        if (remainingAmount <= 0) {
            return 0; // 已充满
        }
        
        return calculateChargingTime(remainingAmount, pile.getChargingPower());
    }
    
    /**
     * 获取指定充电桩的队列请求
     */
    private List<ChargingRequest> getQueueRequests(Integer pileId) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                   .orderBy(true, true, ChargingRequest::getQueuePosition);
        
        return chargingRequestMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取等候区中未分配充电桩的请求
     */
    private List<ChargingRequest> getWaitingRequests() {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                   .isNull(ChargingRequest::getChargingPileId)
                   .orderBy(true, true, ChargingRequest::getCreateTime);
        
        return chargingRequestMapper.selectList(queryWrapper);
    }
    
    private void assignRequestToPile(ChargingRequest request, ChargingPile pile) {
        // 计算在该充电桩的队列位置
        int queuePosition = getQueueRequests(pile.getId()).size() + 1;
        
        // 如果充电桩当前正在充电，队列位置需要加1
        if (ChargingConstants.PILE_STATUS_CHARGING.equals(pile.getStatus())) {
            queuePosition++;
        }
        
        // 更新请求信息
        request.setChargingPileId(pile.getId());
        request.setQueuePosition(queuePosition);
        
        // 保存更新
        chargingRequestMapper.updateById(request);
    }
    
    /**
     * 从配置获取最大队列长度，包含异常处理
     */
    private int getMaxQueueLengthFromConfig() {
        try {
            String queueLengthStr = systemConfigService.getConfigValue(ChargingConstants.CONFIG_PILE_QUEUE_LENGTH);
            if (queueLengthStr != null && !queueLengthStr.trim().isEmpty()) {
                return Integer.parseInt(queueLengthStr.trim());
            }
        } catch (Exception e) {
            // 配置获取失败，使用默认值
        }
        return 2; // 默认队列长度
    }
    
    /**
     * 获取充电桩的最大容量
     */
    private int getMaxCapacityForPile(ChargingPile pile) {
        int maxQueueLength = getMaxQueueLengthFromConfig();
        
        // 获取当前队列中等待的请求数量
        List<ChargingRequest> queueRequests = getQueueRequests(pile.getId());
        int currentQueueSize = queueRequests.size();
        
        if (ChargingConstants.PILE_STATUS_AVAILABLE.equals(pile.getStatus())) {
            // 可用桩：最大容量 - 当前队列中等待的请求数量
            return Math.max(0, maxQueueLength - currentQueueSize);
        } else {
            // 正在充电的桩：最大容量 - 当前队列中等待的请求数量 - 正在充电的车辆(1个)
            return Math.max(0, maxQueueLength - currentQueueSize - 1);
        }
    }
    
    /**
     * 检查充电桩是否有可用容量
     */
    private boolean hasAvailableCapacity(ChargingPile pile, int maxQueueLength) {
        // 获取当前队列中等待的请求数量
        List<ChargingRequest> queueRequests = getQueueRequests(pile.getId());
        int currentQueueSize = queueRequests.size();
        
        if (ChargingConstants.PILE_STATUS_AVAILABLE.equals(pile.getStatus())) {
            // 可用桩：检查是否还有剩余队列容量
            return currentQueueSize < maxQueueLength;
        } else {
            // 正在充电的桩：需要减去正在充电的车辆，检查是否还有剩余队列容量
            return currentQueueSize < maxQueueLength - 1; // -1为正在充电的车辆
        }
    }
    
    /**
     * 全批次最优调度算法
     * 当等候区车辆数不少于充电区全部车位数量时，且充电区没有车辆时，一次性叫号进入充电区
     * 调度策略满足所有车辆完成充电总时长最短
     */
    private void processWaitingRequestsFullBatchOptimal() {
        // 检查充电区是否有车辆（正在充电或等待），如果有则不进行调度
        if (hasVehiclesInChargingArea()) {
            return; // 充电区有车辆，不进行调度
        }
        
        // 获取充电区全部车位数量（可用充电桩数量 × 充电桩队列长度）
        int totalChargingSlots = getTotalChargingSlots();
        if (totalChargingSlots <= 0) {
            return; // 没有可用充电桩
        }
        
        // 获取等候区中未分配充电桩的请求
        List<ChargingRequest> waitingRequests = getWaitingRequests();
        
        // 只有当等候区车辆数不少于充电区全部车位数量时才触发批量调度
        if (waitingRequests.size() < totalChargingSlots) {
            return; // 等候区车辆不足充电区车位数量，不进行批量调度
        }
        
        // 取前N辆车进行批量调度（N为充电区车位数量）
        List<ChargingRequest> batchRequests = waitingRequests.subList(0, totalChargingSlots);
        
        // 获取所有可用充电桩（忽略类型限制）
        List<ChargingPile> allAvailablePiles = getAllAvailablePiles();
        if (allAvailablePiles.isEmpty()) {
            return; // 没有可用充电桩
        }
        
        // 执行全批次优化调度
        executeFullBatchOptimalScheduling(batchRequests, allAvailablePiles);
    }
    
    /**
     * 检查充电区是否有车辆（正在充电或等待）
     * @return 如果充电区有车辆返回true，否则返回false
     */
    private boolean hasVehiclesInChargingArea() {
        // 检查正在充电的车辆
        LambdaQueryWrapper<ChargingRequest> chargingQueryWrapper = new LambdaQueryWrapper<>();
        chargingQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_CHARGING);
        
        Long chargingCount = chargingRequestMapper.selectCount(chargingQueryWrapper);
        if (chargingCount > 0) {
            return true; // 有车辆正在充电
        }
        
        // 检查在充电区等待的车辆（已分配充电桩的等待请求）
        LambdaQueryWrapper<ChargingRequest> waitingQueryWrapper = new LambdaQueryWrapper<>();
        waitingQueryWrapper.eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING)
                          .isNotNull(ChargingRequest::getChargingPileId);
        
        Long waitingInChargingAreaCount = chargingRequestMapper.selectCount(waitingQueryWrapper);
        return waitingInChargingAreaCount > 0; // 有车辆在充电区等待
    }
    
    /**
     * 获取充电区全部车位数量（可用充电桩数量 × 充电桩队列长度）
     * @return 充电区总车位数
     */
    private int getTotalChargingSlots() {
        // 获取所有可用充电桩（排除不可用状态的充电桩）
        List<ChargingPile> availablePiles = chargingPileService.getAllPiles()
                .stream()
                .filter(pile -> !ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(pile.getStatus()))
                .collect(Collectors.toList());
        
        // 获取充电桩队列长度配置
        int maxQueueLength = getMaxQueueLengthFromConfig();
        
        // 充电区全部车位数量 = 可用充电桩数量 × 充电桩队列长度
        return availablePiles.size() * maxQueueLength;
    }
    
    /**
     * 获取所有可用充电桩（忽略类型）
     */
    private List<ChargingPile> getAllAvailablePiles() {
        List<ChargingPile> allPiles = chargingPileService.getAllPiles();
        return allPiles.stream()
                .filter(pile -> !ChargingConstants.PILE_STATUS_UNAVAILABLE.equals(pile.getStatus()))
                .collect(Collectors.toList());
    }
    
    /**
     * 执行全批次优化调度
     * 使用单批次优化调度的算法来处理全批次调度
     */
    private void executeFullBatchOptimalScheduling(List<ChargingRequest> batchRequests, List<ChargingPile> allPiles) {
        // 获取有效的充电桩（有剩余容量的）
        List<ChargingPile> availablePiles = allPiles.stream()
                .filter(pile -> getMaxCapacityForPile(pile) > 0)
                .collect(Collectors.toList());
        
        if (availablePiles.isEmpty()) {
            return;
        }
        
        // 使用单批次优化调度的算法来处理全批次调度
        executeSingleBatchOptimalScheduling(batchRequests, availablePiles);
    }
} 
