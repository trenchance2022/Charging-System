package com.example.backend.service;

import com.example.backend.model.dto.PricingInfoDTO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电计价服务接口
 * 负责分时电价管理、费用计算和价格信息推送
 * 
 * 电价分为三个时段：
 * - 峰时(PEAK): 10:00-15:00, 18:00-21:00, 电价1.0元/度
 * - 平时(NORMAL): 07:00-10:00, 15:00-18:00, 21:00-23:00, 电价0.7元/度  
 * - 谷时(VALLEY): 23:00-07:00, 电价0.4元/度
 * 
 * @author System
 * @since 1.0
 */
public interface PricingService {
    
    /**
     * 获取当前时段的电价信息
     * 
     * @return 当前电价信息DTO，包含价格类型、单价、服务费率等
     */
    PricingInfoDTO getCurrentPricingInfo();
    
    /**
     * 获取所有时段的电价信息
     * 
     * @return 所有时段电价信息列表（峰时、平时、谷时）
     */
    List<PricingInfoDTO> getAllPricingInfo();
    
    /**
     * 计算充电费用（包含电费和服务费）
     * 支持跨时段充电的费用计算
     * 
     * @param chargedAmount 实际充电量（度）
     * @param startTime 开始充电时间字符串
     * @param endTime 结束充电时间字符串
     * @return 总费用（元），包含电费和服务费
     */
    double calculateChargingFee(double chargedAmount, String startTime, String endTime);
    
    /**
     * 启动价格监控定时任务
     * 每分钟检查电价时段变化，自动推送价格更新给所有连接的客户端
     */
    void startPriceMonitoring();
    
    /**
     * 根据指定时间获取电价类型
     * 
     * @param time 指定时间
     * @return 电价类型：PEAK(峰时)、NORMAL(平时)、VALLEY(谷时)
     */
    String getPriceType(LocalDateTime time);
    
    /**
     * 根据电价类型获取单位电价
     * 
     * @param priceType 电价类型（PEAK/NORMAL/VALLEY）
     * @return 单位电价（元/度）
     */
    BigDecimal getUnitPrice(String priceType);
    
    /**
     * 获取服务费率
     * 
     * @return 服务费率（元/度），当前为0.8元/度
     */
    BigDecimal getServiceFeeRate();
} 