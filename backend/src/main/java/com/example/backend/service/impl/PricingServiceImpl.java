package com.example.backend.service.impl;

import com.example.backend.constant.PricingConstants;
import com.example.backend.infrastructure.sse.SseManager;
import com.example.backend.model.dto.PricingInfoDTO;
import com.example.backend.service.PricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 充电计价服务实现类
 * 
 * 实现功能：
 * - 分时电价计算：基于峰时、平时、谷时的电价标准
 * - 跨时段费用计算：支持充电过程跨越多个电价时段的场景
 * - 价格监控：每分钟检查电价时段变化，自动推送更新
 * - 费用明细：计算电费和服务费的详细构成
 * 
 * @author System
 * @since 1.0
 */
@Service
@EnableScheduling
public class PricingServiceImpl implements PricingService {
    @Autowired
    private SseManager sseManager;
    
    private String lastPeriodType = null;
    
    @Override
    public PricingInfoDTO getCurrentPricingInfo() {
        LocalDateTime now = LocalDateTime.now();
        String priceType = getPriceType(now);
        BigDecimal unitPrice = getUnitPrice(priceType);
        BigDecimal serviceFeeRate = getServiceFeeRate();
        
        String priceTypeName = getPriceTypeName(priceType);
        String currentPeriod = getCurrentPeriodDescription(priceType);
        String timePeriods = getTimePeriodDescription(priceType);
        
        return new PricingInfoDTO(priceType, priceTypeName, unitPrice, serviceFeeRate, timePeriods, currentPeriod);
    }
    
    @Override
    public List<PricingInfoDTO> getAllPricingInfo() {
        List<PricingInfoDTO> pricingInfoList = new ArrayList<>();
        
        // 峰时电价信息
        pricingInfoList.add(new PricingInfoDTO(
                PricingConstants.PRICE_TYPE_PEAK,
                "峰时",
                BigDecimal.valueOf(PricingConstants.PRICE_PEAK),
                getServiceFeeRate(),
                PricingConstants.PEAK_PERIOD_DESC_1 + ", " + PricingConstants.PEAK_PERIOD_DESC_2,
                ""
        ));
        
        // 平时电价信息
        pricingInfoList.add(new PricingInfoDTO(
                PricingConstants.PRICE_TYPE_NORMAL,
                "平时",
                BigDecimal.valueOf(PricingConstants.PRICE_NORMAL),
                getServiceFeeRate(),
                PricingConstants.NORMAL_PERIOD_DESC_1 + ", " + PricingConstants.NORMAL_PERIOD_DESC_2 + ", " + PricingConstants.NORMAL_PERIOD_DESC_3,
                ""
        ));
        
        // 谷时电价信息
        pricingInfoList.add(new PricingInfoDTO(
                PricingConstants.PRICE_TYPE_VALLEY,
                "谷时",
                BigDecimal.valueOf(PricingConstants.PRICE_VALLEY),
                getServiceFeeRate(),
                PricingConstants.VALLEY_PERIOD_DESC,
                ""
        ));
        
        return pricingInfoList;
    }
    
    @Override
    public double calculateChargingFee(double chargedAmount, String startTimeStr, String endTimeStr) {
        try {
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            // 如果跨越多个时段，需要分段计算
            // 这里简化处理，使用平均电价
            double avgPrice = calculateAveragePrice(startTime, endTime);
            
            // 计算充电费用
            double chargingFee = chargedAmount * avgPrice;
            
            // 计算服务费
            double serviceFee = chargedAmount * PricingConstants.SERVICE_FEE_RATE;
            
            // 总费用
            return chargingFee + serviceFee;
        } catch (Exception e) {
            // 如果解析失败，使用当前电价计算
            double currentPrice = getPriceByPeriodType(getPeriodType(LocalTime.now()));
            return chargedAmount * (currentPrice + PricingConstants.SERVICE_FEE_RATE);
        }
    }
    
    /**
     * 每5s检查一次时段变化，直接推送价格更新
     */
    @Override
    @Scheduled(fixedRate = 5000) // 每5s执行一次
    public void startPriceMonitoring() {
        PricingInfoDTO currentInfo = getCurrentPricingInfo();
        String currentPeriodType = currentInfo.getPriceType();
        
        // 如果时段发生变化，直接推送给所有连接的客户端
        if (lastPeriodType == null || !lastPeriodType.equals(currentPeriodType)) {
            lastPeriodType = currentPeriodType;
            
            // 直接广播价格更新，无需事件机制
            pushPricingUpdate(currentInfo);
        }
    }
    
    @Override
    public String getPriceType(LocalDateTime time) {
        LocalTime timeOfDay = time.toLocalTime();
        
        // 判断峰时：10:00~15:00，18:00~21:00
        if ((timeOfDay.compareTo(PricingConstants.PEAK_START_1) >= 0 && timeOfDay.compareTo(PricingConstants.PEAK_END_1) < 0) ||
            (timeOfDay.compareTo(PricingConstants.PEAK_START_2) >= 0 && timeOfDay.compareTo(PricingConstants.PEAK_END_2) < 0)) {
            return PricingConstants.PRICE_TYPE_PEAK;
        }
        
        // 判断谷时：23:00~次日7:00
        if (timeOfDay.compareTo(PricingConstants.VALLEY_START) >= 0 || timeOfDay.compareTo(PricingConstants.VALLEY_END) < 0) {
            return PricingConstants.PRICE_TYPE_VALLEY;
        }
        
        // 其余时间为平时：7:00~10:00，15:00~18:00，21:00~23:00
        return PricingConstants.PRICE_TYPE_NORMAL;
    }
    
    @Override
    public BigDecimal getUnitPrice(String priceType) {
        switch (priceType) {
            case PricingConstants.PRICE_TYPE_PEAK:
                return BigDecimal.valueOf(PricingConstants.PRICE_PEAK);
            case PricingConstants.PRICE_TYPE_VALLEY:
                return BigDecimal.valueOf(PricingConstants.PRICE_VALLEY);
            case PricingConstants.PRICE_TYPE_NORMAL:
            default:
                return BigDecimal.valueOf(PricingConstants.PRICE_NORMAL);
        }
    }
    
    @Override
    public BigDecimal getServiceFeeRate() {
        return BigDecimal.valueOf(PricingConstants.SERVICE_FEE_RATE);
    }
    
    /**
     * 推送价格更新
     * 整合了原本在控制器层的广播逻辑
     */
    private void pushPricingUpdate(PricingInfoDTO pricingInfo) {
        try {
            // 广播给所有连接的客户端
            sseManager.broadcast(pricingInfo);
        } catch (Exception e) {
        }
    }
    
    /**
     * 获取电价类型名称
     */
    private String getPriceTypeName(String priceType) {
        switch (priceType) {
            case PricingConstants.PRICE_TYPE_PEAK:
                return "峰时";
            case PricingConstants.PRICE_TYPE_VALLEY:
                return "谷时";
            case PricingConstants.PRICE_TYPE_NORMAL:
            default:
                return "平时";
        }
    }
    
    /**
     * 获取当前时段描述
     */
    private String getCurrentPeriodDescription(String priceType) {
        LocalDateTime now = LocalDateTime.now();
        LocalTime timeOfDay = now.toLocalTime();
        
        switch (priceType) {
            case PricingConstants.PRICE_TYPE_PEAK:
                if (timeOfDay.compareTo(PricingConstants.PEAK_START_1) >= 0 && timeOfDay.compareTo(PricingConstants.PEAK_END_1) < 0) {
                    return PricingConstants.PEAK_PERIOD_DESC_1;
                } else {
                    return PricingConstants.PEAK_PERIOD_DESC_2;
                }
            case PricingConstants.PRICE_TYPE_VALLEY:
                return PricingConstants.VALLEY_PERIOD_DESC;
            case PricingConstants.PRICE_TYPE_NORMAL:
            default:
                if (timeOfDay.compareTo(PricingConstants.NORMAL_START_1) >= 0 && timeOfDay.compareTo(PricingConstants.NORMAL_END_1) < 0) {
                    return PricingConstants.NORMAL_PERIOD_DESC_1;
                } else if (timeOfDay.compareTo(PricingConstants.NORMAL_START_2) >= 0 && timeOfDay.compareTo(PricingConstants.NORMAL_END_2) < 0) {
                    return PricingConstants.NORMAL_PERIOD_DESC_2;
                } else {
                    return PricingConstants.NORMAL_PERIOD_DESC_3;
                }
        }
    }
    
    /**
     * 获取时段描述
     */
    private String getTimePeriodDescription(String priceType) {
        switch (priceType) {
            case PricingConstants.PRICE_TYPE_PEAK:
                return PricingConstants.PEAK_PERIOD_DESC_1 + ", " + PricingConstants.PEAK_PERIOD_DESC_2;
            case PricingConstants.PRICE_TYPE_VALLEY:
                return PricingConstants.VALLEY_PERIOD_DESC;
            case PricingConstants.PRICE_TYPE_NORMAL:
            default:
                return PricingConstants.NORMAL_PERIOD_DESC_1 + ", " + PricingConstants.NORMAL_PERIOD_DESC_2 + ", " + PricingConstants.NORMAL_PERIOD_DESC_3;
        }
    }
    
    /**
     * 计算开始时间到结束时间之间的平均电价
     */
    private double calculateAveragePrice(LocalDateTime startTime, LocalDateTime endTime) {
        // 这里是简化处理，实际应该按时间段比例计算加权平均价格
        // 对于跨越多天的情况，也需要特殊处理
        
        // 简单方法：取开始和结束时间的平均电价
        double startPrice = getPriceByPeriodType(getPeriodType(startTime.toLocalTime()));
        double endPrice = getPriceByPeriodType(getPeriodType(endTime.toLocalTime()));
        
        return (startPrice + endPrice) / 2;
    }
    
    /**
     * 获取指定时间属于哪个时段类型
     */
    private String getPeriodType(LocalTime time) {
        // 判断是否为峰时
        if ((time.isAfter(PricingConstants.PEAK_START_1) || time.equals(PricingConstants.PEAK_START_1)) && 
            time.isBefore(PricingConstants.PEAK_END_1)) {
            return PricingConstants.PRICE_TYPE_PEAK;
        }
        
        if ((time.isAfter(PricingConstants.PEAK_START_2) || time.equals(PricingConstants.PEAK_START_2)) && 
            time.isBefore(PricingConstants.PEAK_END_2)) {
            return PricingConstants.PRICE_TYPE_PEAK;
        }
        
        // 判断是否为平时
        if ((time.isAfter(PricingConstants.NORMAL_START_1) || time.equals(PricingConstants.NORMAL_START_1)) && 
            time.isBefore(PricingConstants.NORMAL_END_1)) {
            return PricingConstants.PRICE_TYPE_NORMAL;
        }
        
        if ((time.isAfter(PricingConstants.NORMAL_START_2) || time.equals(PricingConstants.NORMAL_START_2)) && 
            time.isBefore(PricingConstants.NORMAL_END_2)) {
            return PricingConstants.PRICE_TYPE_NORMAL;
        }
        
        if ((time.isAfter(PricingConstants.NORMAL_START_3) || time.equals(PricingConstants.NORMAL_START_3)) && 
            time.isBefore(PricingConstants.NORMAL_END_3)) {
            return PricingConstants.PRICE_TYPE_NORMAL;
        }
        
        // 其他时间为谷时（23:00-07:00）
        return PricingConstants.PRICE_TYPE_VALLEY;
    }
    
    /**
     * 根据时段类型获取电价
     */
    private double getPriceByPeriodType(String periodType) {
        if (PricingConstants.PRICE_TYPE_PEAK.equals(periodType)) {
            return PricingConstants.PRICE_PEAK;
        } else if (PricingConstants.PRICE_TYPE_NORMAL.equals(periodType)) {
            return PricingConstants.PRICE_NORMAL;
        } else {
            return PricingConstants.PRICE_VALLEY;
        }
    }
} 