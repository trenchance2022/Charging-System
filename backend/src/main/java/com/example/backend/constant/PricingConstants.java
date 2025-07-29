package com.example.backend.constant;

import java.time.LocalTime;

/**
 * 充电计费常量
 */
public class PricingConstants {
    // 电价类型
    public static final String PRICE_TYPE_PEAK = "PEAK";       // 峰时
    public static final String PRICE_TYPE_NORMAL = "NORMAL";   // 平时
    public static final String PRICE_TYPE_VALLEY = "VALLEY";   // 谷时
    
    // 电价（元/度）
    public static final double PRICE_PEAK = 1.0;    // 峰时电价
    public static final double PRICE_NORMAL = 0.7;  // 平时电价
    public static final double PRICE_VALLEY = 0.4;  // 谷时电价
    
    // 服务费（元/度）
    public static final double SERVICE_FEE_RATE = 0.8;
    
    // 时间段定义
    // 峰时：10:00~15:00，18:00~21:00
    public static final LocalTime PEAK_START_1 = LocalTime.of(10, 0);
    public static final LocalTime PEAK_END_1 = LocalTime.of(15, 0);
    public static final LocalTime PEAK_START_2 = LocalTime.of(18, 0);
    public static final LocalTime PEAK_END_2 = LocalTime.of(21, 0);
    
    // 平时：7:00~10:00，15:00~18:00，21:00~23:00
    public static final LocalTime NORMAL_START_1 = LocalTime.of(7, 0);
    public static final LocalTime NORMAL_END_1 = LocalTime.of(10, 0);
    public static final LocalTime NORMAL_START_2 = LocalTime.of(15, 0);
    public static final LocalTime NORMAL_END_2 = LocalTime.of(18, 0);
    public static final LocalTime NORMAL_START_3 = LocalTime.of(21, 0);
    public static final LocalTime NORMAL_END_3 = LocalTime.of(23, 0);
    
    // 谷时：23:00~次日7:00
    public static final LocalTime VALLEY_START = LocalTime.of(23, 0);
    public static final LocalTime VALLEY_END = LocalTime.of(7, 0);
    
    // 时段描述
    public static final String PEAK_PERIOD_DESC_1 = "10:00 - 15:00";
    public static final String PEAK_PERIOD_DESC_2 = "18:00 - 21:00";
    public static final String NORMAL_PERIOD_DESC_1 = "07:00 - 10:00";
    public static final String NORMAL_PERIOD_DESC_2 = "15:00 - 18:00";
    public static final String NORMAL_PERIOD_DESC_3 = "21:00 - 23:00";
    public static final String VALLEY_PERIOD_DESC = "23:00 - 07:00";
} 