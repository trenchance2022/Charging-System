package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.constant.ChargingConstants;
import com.example.backend.mapper.ChargingPileMapper;
import com.example.backend.mapper.ChargingRequestMapper;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingPileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ChargingPileServiceImpl extends ServiceImpl<ChargingPileMapper, ChargingPile> implements ChargingPileService {
    
    @Autowired
    private ChargingRequestMapper chargingRequestMapper;
    
    @Override
    public List<ChargingPile> getAllPiles() {
        return list();
    }
    
    @Override
    public List<ChargingPile> getPilesByType(String type) {
        LambdaQueryWrapper<ChargingPile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingPile::getPileType, type);
        return list(queryWrapper);
    }
    
    @Override
    public int getQueueLengthByPileId(Integer pileId) {
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .eq(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING);
        
        Long count = chargingRequestMapper.selectCount(queryWrapper);
        return count.intValue();
    }
    
    @Override
    public double calculateWaitingTime(Integer pileId) {
        // 获取该充电桩的所有等待和充电中的请求
        LambdaQueryWrapper<ChargingRequest> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingRequest::getChargingPileId, pileId)
                   .in(ChargingRequest::getStatus, ChargingConstants.STATUS_WAITING, ChargingConstants.STATUS_CHARGING)
                   .orderBy(true, true, ChargingRequest::getQueuePosition);
        
        List<ChargingRequest> requests = chargingRequestMapper.selectList(queryWrapper);
        
        if (requests.isEmpty()) {
            return 0.0;
        }
        
        // 获取充电桩信息
        ChargingPile pile = getById(pileId);
        if (pile == null) {
            return 0.0;
        }
        
        // 计算总等待时间（分钟）
        double totalWaitingTime = 0.0;
        for (ChargingRequest request : requests) {
            // 充电时间 = 充电量 / 充电功率 * 60分钟
            double chargingTime = (request.getChargingAmount() / pile.getChargingPower()) * 60;
            totalWaitingTime += chargingTime;
        }
        
        return totalWaitingTime;
    }
    
    @Override
    public List<ChargingPile> getAvailablePilesByType(String pileType) {
        LambdaQueryWrapper<ChargingPile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingPile::getPileType, pileType)
                   .in(ChargingPile::getStatus, 
                       ChargingConstants.PILE_STATUS_AVAILABLE, 
                       ChargingConstants.PILE_STATUS_CHARGING);
        
        return list(queryWrapper);
    }
    
    @Override
    public ChargingPile findOptimalPile(String chargingMode, Double chargingAmount) {
        // 根据充电模式确定充电桩类型
        String pileType = ChargingConstants.MODE_FAST.equals(chargingMode) ? 
                ChargingConstants.PILE_TYPE_FAST : ChargingConstants.PILE_TYPE_SLOW;
        
        // 获取对应类型的可用充电桩
        List<ChargingPile> availablePiles = getAvailablePilesByType(pileType);
        if (availablePiles.isEmpty()) {
            return null;
        }
        
        // 计算每个充电桩的总完成时间（等待时间 + 自己充电时间）
        Optional<ChargingPile> optimalPile = availablePiles.stream().min(Comparator.comparingDouble(pile -> {
            double waitingTime = calculateWaitingTime(pile.getId());
            double selfChargingTime = (chargingAmount / pile.getChargingPower()) * 60; // 转换为分钟
            return waitingTime + selfChargingTime;
        }));
        
        return optimalPile.orElse(null);
    }
    
    @Override
    public boolean togglePileStatus(String pileNumber) {
        try {
            // 查找充电桩
            LambdaQueryWrapper<ChargingPile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChargingPile::getPileNumber, pileNumber);
            ChargingPile pile = getOne(queryWrapper);
            
            if (pile == null) {
                return false;
            }
            
            // 切换状态
            String currentStatus = pile.getStatus();
            String newStatus;
            
            if (ChargingConstants.PILE_STATUS_AVAILABLE.equals(currentStatus) || 
                ChargingConstants.PILE_STATUS_CHARGING.equals(currentStatus)) {
                // 当前是开启状态，切换为关闭
                newStatus = ChargingConstants.PILE_STATUS_UNAVAILABLE;
            } else {
                // 当前是关闭状态，切换为开启
                newStatus = ChargingConstants.PILE_STATUS_AVAILABLE;
            }
            
            pile.setStatus(newStatus);
            return updateById(pile);
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public ChargingPile addChargingPile(String pileType) {
        try {
            // 验证充电桩类型
            if (!ChargingConstants.PILE_TYPE_FAST.equals(pileType) && 
                !ChargingConstants.PILE_TYPE_SLOW.equals(pileType)) {
                throw new IllegalArgumentException("无效的充电桩类型: " + pileType);
            }
            
            // 生成新的充电桩编号
            String pileNumber = generatePileNumber(pileType);
            
            // 根据类型设置充电功率
            Double chargingPower;
            if (ChargingConstants.PILE_TYPE_FAST.equals(pileType)) {
                chargingPower = 30.0; // 快充功率 30kW
            } else {
                chargingPower = 7.0; // 慢充功率 7kW
            }
            
            // 创建新充电桩
            ChargingPile newPile = new ChargingPile(
                pileNumber,
                pileType,
                chargingPower,
                ChargingConstants.PILE_STATUS_AVAILABLE
            );
            
            // 保存到数据库
            boolean saved = save(newPile);
            if (saved) {
                return newPile;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public boolean deleteChargingPile(String pileNumber) {
        try {
            // 查找充电桩
            LambdaQueryWrapper<ChargingPile> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ChargingPile::getPileNumber, pileNumber);
            ChargingPile pile = getOne(queryWrapper);
            
            if (pile == null) {
                return false;
            }
            
            // 检查充电桩是否正在使用中
            if (ChargingConstants.PILE_STATUS_CHARGING.equals(pile.getStatus())) {
                throw new IllegalStateException("充电桩正在使用中，无法删除");
            }
            
            // 检查是否有等待队列
            int queueLength = getQueueLengthByPileId(pile.getId());
            if (queueLength > 0) {
                throw new IllegalStateException("充电桩还有等待队列，无法删除");
            }
            
            // 删除充电桩
            return removeById(pile.getId());
        } catch (Exception e) {
            throw new RuntimeException("删除充电桩失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成充电桩编号
     * @param pileType 充电桩类型
     * @return 新的充电桩编号
     */
    private String generatePileNumber(String pileType) {
        String prefix = ChargingConstants.PILE_TYPE_FAST.equals(pileType) ? "F" : "T";
        
        // 查找同类型的最大编号
        LambdaQueryWrapper<ChargingPile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingPile::getPileType, pileType)
                   .orderByDesc(ChargingPile::getPileNumber)
                   .last("LIMIT 1");
        
        ChargingPile latestPile = getOne(queryWrapper);
        int nextSeq = 1;
        
        if (latestPile != null && latestPile.getPileNumber() != null) {
            String pileNumber = latestPile.getPileNumber();
            // 提取数字部分
            String numPart = pileNumber.substring(1);
            try {
                nextSeq = Integer.parseInt(numPart) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }
        
        return prefix + String.format("%03d", nextSeq);
    }
} 