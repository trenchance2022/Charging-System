package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.model.entity.ChargingBill;
import com.example.backend.model.entity.ChargingRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 充电详单服务接口
 */
public interface ChargingBillService extends IService<ChargingBill> {
    
    /**
     * 生成充电详单
     * 在充电结束时调用，生成详细的充电账单
     * 
     * @param request 充电请求
     * @return 生成的充电详单
     */
    ChargingBill generateBill(ChargingRequest request);
    
    /**
     * 根据用户ID获取充电详单列表
     * 
     * @param userId 用户ID
     * @return 充电详单列表
     */
    List<ChargingBill> getBillsByUserId(Long userId);
    
    /**
     * 生成详单编号
     * 格式：BILL + 年月日 + 4位序号，如 BILL202312010001
     * 
     * @return 详单编号
     */
    String generateBillNumber();
    
    /**
     * 使用分时电价计算充电费用
     * 支持跨时段充电的费用计算
     * 
     * @param startTime 开始充电时间
     * @param endTime 结束充电时间
     * @param chargedAmount 充电量（kWh）
     * @return 充电费用（不含服务费）
     */
    BigDecimal calculateChargingFeeWithTimeBasedPricing(LocalDateTime startTime, LocalDateTime endTime, Double chargedAmount);
    
    /**
     * 获取充电桩的统计信息
     * 
     * @param pileNumber 充电桩编号
     * @return 包含累计充电次数、时长、电量的统计信息
     */
    Map<String, Object> getPileStatistics(String pileNumber);
    
    /**
     * 按时间段获取充电桩报表统计
     * 
     * @param startDate 开始日期 (格式: yyyy-MM-dd)
     * @param endDate 结束日期 (格式: yyyy-MM-dd)
     * @return 按充电桩分组的统计报表数据
     */
    List<Map<String, Object>> getReportStatistics(String startDate, String endDate);
} 