package com.example.backend.constant;

/**
 * 充电系统常量
 */
public class ChargingConstants {
    // 充电模式
    public static final String MODE_FAST = "fast";  // 快充
    public static final String MODE_SLOW = "slow";  // 慢充
    
    // 充电请求状态
    public static final String STATUS_WAITING = "WAITING";  // 等待中
    public static final String STATUS_PRIORITY_WAITING = "PRIORITY_WAITING";  // 优先等待中
    public static final String STATUS_CHARGING = "CHARGING";  // 充电中
    public static final String STATUS_COMPLETED = "COMPLETED";  // 已完成
    public static final String STATUS_CANCELED = "CANCELED";  // 已取消
    
    // 充电桩类型
    public static final String PILE_TYPE_FAST = "FAST";  // 快充桩
    public static final String PILE_TYPE_SLOW = "SLOW";  // 慢充桩
    
    // 充电桩状态
    public static final String PILE_STATUS_AVAILABLE = "AVAILABLE";  // 可用
    public static final String PILE_STATUS_CHARGING = "CHARGING";  // 充电中
    public static final String PILE_STATUS_UNAVAILABLE = "UNAVAILABLE";  // 不可用

    // 系统配置键
    public static final String CONFIG_WAITING_AREA_CAPACITY = "waiting_area_capacity";  // 等候区容量
    public static final String CONFIG_PILE_QUEUE_LENGTH = "pile_queue_length";  // 充电桩队列长度
    public static final String CONFIG_FAST_CHARGING_POWER = "fast_charging_power";  // 快充功率
    public static final String CONFIG_SLOW_CHARGING_POWER = "slow_charging_power";  // 慢充功率
    public static final String CONFIG_SCHEDULE_STRATEGY = "schedule_strategy";  // 调度策略配置键
    public static final String CONFIG_FAST_BATCH_THRESHOLD = "fast_batch_threshold";  // 快充批次调度触发阈值
    public static final String CONFIG_SLOW_BATCH_THRESHOLD = "slow_batch_threshold";  // 慢充批次调度触发阈值
    
    // 调度策略
    public static final String SCHEDULE_STRATEGY_ORIGINAL = "ORIGINAL";  // 原始算法
    public static final String SCHEDULE_STRATEGY_SINGLE_BATCH_OPTIMAL = "SINGLE_BATCH_OPTIMAL";  // 单批次最优
    public static final String SCHEDULE_STRATEGY_FULL_BATCH_OPTIMAL = "FULL_BATCH_OPTIMAL";  // 全批次最优
    
    // 请求ID前缀
    public static final String REQUEST_PREFIX_FAST = "F";  // 快充请求前缀
    public static final String REQUEST_PREFIX_SLOW = "T";  // 慢充请求前缀
} 