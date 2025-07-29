package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.mapper.ChargingBillMapper;
import com.example.backend.mapper.ChargingPileMapper;
import com.example.backend.model.entity.ChargingBill;
import com.example.backend.model.entity.ChargingPile;
import com.example.backend.model.entity.ChargingRequest;
import com.example.backend.service.ChargingBillService;
import com.example.backend.service.PricingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChargingBillServiceImpl extends ServiceImpl<ChargingBillMapper, ChargingBill> implements ChargingBillService {
    @Autowired
    private ChargingPileMapper chargingPileMapper;
    
    @Autowired
    private PricingService pricingService;
    
    @Override
    public ChargingBill generateBill(ChargingRequest request) {
        if (request == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("充电请求信息不完整，无法生成详单");
        }
        
        // 获取充电桩信息
        ChargingPile pile = chargingPileMapper.selectById(request.getChargingPileId());
        if (pile == null) {
            throw new IllegalArgumentException("找不到对应的充电桩信息");
        }
        
        // 计算充电时长（分钟）
        Integer chargingDuration = (int) ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime());
        
        // 计算实际充电量
        Double chargedAmount = calculateChargedAmount(request, pile, chargingDuration);
        
        // 使用分时电价计算费用
        BigDecimal chargingFee = calculateChargingFeeWithTimeBasedPricing(
                request.getStartTime(), request.getEndTime(), chargedAmount);
        BigDecimal serviceFee = calculateServiceFee(chargedAmount);
        BigDecimal totalFee = chargingFee.add(serviceFee);
        
        // 生成详单编号
        String billNumber = generateBillNumber();
        
        // 创建充电详单
        ChargingBill bill = new ChargingBill(
                billNumber,
                request.getRequestId(),
                request.getUserId(),
                pile.getPileNumber(),
                chargedAmount,
                chargingDuration,
                request.getStartTime(),
                request.getEndTime(),
                chargingFee,
                serviceFee,
                totalFee,
                request.getChargingMode(),
                pile.getChargingPower()
        );
        
        // 保存详单
        save(bill);
        
        return bill;
    }
    
    @Override
    public List<ChargingBill> getBillsByUserId(Long userId) {
        LambdaQueryWrapper<ChargingBill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingBill::getUserId, userId)
                   .orderBy(true, false, ChargingBill::getBillTime);
        
        return list(queryWrapper);
    }
    
    @Override
    public String generateBillNumber() {
        // 格式：BILL + 年月日 + 4位序号
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "BILL" + dateStr;
        
        // 查询当天最大序号
        LambdaQueryWrapper<ChargingBill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.likeRight(ChargingBill::getBillNumber, prefix)
                   .orderByDesc(ChargingBill::getBillNumber)
                   .last("LIMIT 1");
        
        ChargingBill latestBill = getOne(queryWrapper);
        int nextSeq = 1;
        
        if (latestBill != null && latestBill.getBillNumber() != null) {
            String billNumber = latestBill.getBillNumber();
            // 提取序号部分
            String seqPart = billNumber.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                nextSeq = 1;
            }
        }
        
        return prefix + String.format("%04d", nextSeq);
    }
    
    /**
     * 计算实际充电量
     */
    private Double calculateChargedAmount(ChargingRequest request, ChargingPile pile, Integer chargingDuration) {
        // 实际充电量 = 充电功率 * 充电时长 / 60
        double calculatedAmount = (pile.getChargingPower() * chargingDuration) / 60.0;
        
        // 不能超过请求的充电量
        return Math.min(calculatedAmount, request.getChargingAmount());
    }
    
    /**
     * 使用分时电价计算充电费用
     */
    @Override
    public BigDecimal calculateChargingFeeWithTimeBasedPricing(LocalDateTime startTime, LocalDateTime endTime, Double chargedAmount) {
        if (startTime == null || endTime == null || chargedAmount == null || chargedAmount <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 将时间截断到分钟级别，忽略秒数
        LocalDateTime adjustedStartTime = startTime.withSecond(0).withNano(0);
        LocalDateTime adjustedEndTime = endTime.withSecond(0).withNano(0);
        
        // 如果截断后时间相同，说明充电时长不足1分钟，向上取整到1分钟
        if (adjustedStartTime.equals(adjustedEndTime)) {
            adjustedEndTime = adjustedStartTime.plusMinutes(1);
        }
        
        // 计算总充电时长（分钟）
        long totalMinutes = ChronoUnit.MINUTES.between(adjustedStartTime, adjustedEndTime);
        if (totalMinutes <= 0) {
            return BigDecimal.ZERO;
        }
        
        // 获取所有时间节点（包括起始时间和电价变更时间点）
        List<LocalDateTime> timePoints = getTimePoints(adjustedStartTime, adjustedEndTime);
        
        BigDecimal totalChargingFee = BigDecimal.ZERO;
        
        // 遍历每个时间段，计算各时段的充电费用
        for (int i = 0; i < timePoints.size() - 1; i++) {
            LocalDateTime segmentStart = timePoints.get(i);
            LocalDateTime segmentEnd = timePoints.get(i + 1);
            
            // 计算该时段的充电时长（分钟）
            long segmentMinutes = ChronoUnit.MINUTES.between(segmentStart, segmentEnd);
            
            // 计算该时段的充电量比例
            double segmentRatio = (double) segmentMinutes / totalMinutes;
            double segmentChargedAmount = chargedAmount * segmentRatio;
            
            // 获取该时段的电价类型和单价
            String priceType = pricingService.getPriceType(segmentStart);
            BigDecimal unitPrice = pricingService.getUnitPrice(priceType);
            
            // 计算该时段的充电费用
            BigDecimal segmentFee = unitPrice.multiply(BigDecimal.valueOf(segmentChargedAmount))
                    .setScale(2, RoundingMode.HALF_UP);
            
            totalChargingFee = totalChargingFee.add(segmentFee);
        }
        
        return totalChargingFee.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 获取起止时间内所有的时间节点（包括电价变更时间点）
     */
    private List<LocalDateTime> getTimePoints(LocalDateTime startTime, LocalDateTime endTime) {
        List<LocalDateTime> timePoints = new ArrayList<>();
        timePoints.add(startTime);
        
        // 定义一天内所有电价变更时间点
        LocalTime[] priceChangePoints = {
                LocalTime.of(7, 0),   // 谷时 -> 平时
                LocalTime.of(10, 0),  // 平时 -> 峰时
                LocalTime.of(15, 0),  // 峰时 -> 平时
                LocalTime.of(18, 0),  // 平时 -> 峰时
                LocalTime.of(21, 0),  // 峰时 -> 平时
                LocalTime.of(23, 0)   // 平时 -> 谷时
        };
        
        // 遍历充电时间段内的每一天
        LocalDateTime currentDay = startTime.toLocalDate().atStartOfDay();
        LocalDateTime endDay = endTime.toLocalDate().atStartOfDay().plusDays(1);
        
        while (currentDay.isBefore(endDay)) {
            // 检查当天的每个电价变更时间点
            for (LocalTime changePoint : priceChangePoints) {
                LocalDateTime changeDateTime = currentDay.with(changePoint);
                
                // 添加在充电时间段内的变更点（不包括起始时间）
                if (changeDateTime.isAfter(startTime) && changeDateTime.isBefore(endTime)) {
                    timePoints.add(changeDateTime);
                }
            }
            currentDay = currentDay.plusDays(1);
        }
        
        timePoints.add(endTime);
        
        // 按时间排序并去重
        timePoints.sort(LocalDateTime::compareTo);
        
        // 去重处理，避免重复的时间点
        List<LocalDateTime> uniqueTimePoints = new ArrayList<>();
        for (LocalDateTime timePoint : timePoints) {
            if (uniqueTimePoints.isEmpty() || !uniqueTimePoints.get(uniqueTimePoints.size() - 1).equals(timePoint)) {
                uniqueTimePoints.add(timePoint);
            }
        }
        
        return uniqueTimePoints;
    }
    
    /**
     * 计算服务费用
     */
    private BigDecimal calculateServiceFee(Double chargedAmount) {
        // 服务费 = 服务费率 * 充电量
        BigDecimal serviceFeeRate = pricingService.getServiceFeeRate();
        
        return serviceFeeRate.multiply(BigDecimal.valueOf(chargedAmount))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    @Override
    public Map<String, Object> getPileStatistics(String pileNumber) {
        LambdaQueryWrapper<ChargingBill> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChargingBill::getPileNumber, pileNumber);
        
        List<ChargingBill> bills = list(queryWrapper);
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (bills.isEmpty()) {
            statistics.put("totalCharges", 0);
            statistics.put("totalTime", 0);
            statistics.put("totalPower", 0.0);
        } else {
            // 累计充电次数
            statistics.put("totalCharges", bills.size());
            
            // 累计充电时长（分钟）
            int totalTime = bills.stream()
                .mapToInt(bill -> bill.getChargingDuration() != null ? bill.getChargingDuration() : 0)
                .sum();
            statistics.put("totalTime", totalTime);
            
            // 累计充电电量（kWh）
            double totalPower = bills.stream()
                .mapToDouble(bill -> bill.getChargedAmount() != null ? bill.getChargedAmount() : 0.0)
                .sum();
            statistics.put("totalPower", Math.round(totalPower * 100.0) / 100.0); // 保留两位小数
        }
        
        return statistics;
    }
    
    @Override
    public List<Map<String, Object>> getReportStatistics(String startDate, String endDate) {
        try {
            // 解析日期
            LocalDateTime startDateTime = LocalDateTime.parse(startDate + " 00:00:00", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime endDateTime = LocalDateTime.parse(endDate + " 23:59:59", 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            // 查询指定时间段内的所有账单
            LambdaQueryWrapper<ChargingBill> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.between(ChargingBill::getBillTime, startDateTime, endDateTime);
            
            List<ChargingBill> bills = list(queryWrapper);
            
            // 按充电桩分组统计
            Map<String, List<ChargingBill>> billsByPile = bills.stream()
                .collect(Collectors.groupingBy(ChargingBill::getPileNumber));
            
            List<Map<String, Object>> reportData = new ArrayList<>();
            
            for (Map.Entry<String, List<ChargingBill>> entry : billsByPile.entrySet()) {
                String pileNumber = entry.getKey();
                List<ChargingBill> pileBills = entry.getValue();
                
                Map<String, Object> pileStats = new HashMap<>();
                pileStats.put("pileNumber", pileNumber);
                
                // 累计充电次数
                pileStats.put("totalCharges", pileBills.size());
                
                // 累计充电时长（分钟）
                int totalTime = pileBills.stream()
                    .mapToInt(bill -> bill.getChargingDuration() != null ? bill.getChargingDuration() : 0)
                    .sum();
                pileStats.put("totalTime", totalTime);
                
                // 累计充电电量（kWh）
                double totalPower = pileBills.stream()
                    .mapToDouble(bill -> bill.getChargedAmount() != null ? bill.getChargedAmount() : 0.0)
                    .sum();
                pileStats.put("totalPower", Math.round(totalPower * 100.0) / 100.0);
                
                // 累计充电费用（元）
                BigDecimal totalChargingFee = pileBills.stream()
                    .map(bill -> bill.getChargingFee() != null ? bill.getChargingFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                pileStats.put("totalChargingFee", totalChargingFee.setScale(2, RoundingMode.HALF_UP));
                
                // 累计服务费用（元）
                BigDecimal totalServiceFee = pileBills.stream()
                    .map(bill -> bill.getServiceFee() != null ? bill.getServiceFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                pileStats.put("totalServiceFee", totalServiceFee.setScale(2, RoundingMode.HALF_UP));
                
                // 累计总费用（元）
                BigDecimal totalFee = pileBills.stream()
                    .map(bill -> bill.getTotalFee() != null ? bill.getTotalFee() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                pileStats.put("totalFee", totalFee.setScale(2, RoundingMode.HALF_UP));
                
                reportData.add(pileStats);
            }
            
            // 按充电桩编号排序
            reportData.sort((a, b) -> String.valueOf(a.get("pileNumber"))
                .compareTo(String.valueOf(b.get("pileNumber"))));
            
            return reportData;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
} 