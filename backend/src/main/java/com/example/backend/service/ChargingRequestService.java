package com.example.backend.service;

import com.example.backend.model.dto.ChargingRequestDTO;
import com.example.backend.model.dto.ChargingResponseDTO;
import com.example.backend.model.dto.ChargingStatusDTO;
import com.example.backend.model.entity.ChargingRequest;

import java.util.List;
import java.util.Map;

public interface ChargingRequestService {
    
    /**
     * 提交充电请求
     * @param userId 用户ID
     * @param requestDTO 充电请求DTO
     * @return 充电响应DTO
     */
    ChargingResponseDTO submitRequest(Long userId, ChargingRequestDTO requestDTO);
    
    /**
     * 修改用户的活跃充电请求
     * @param userId 用户ID
     * @param requestDTO 充电请求DTO
     * @return 充电响应DTO
     */
    ChargingResponseDTO modifyActiveRequest(Long userId, ChargingRequestDTO requestDTO);
    
    /**
     * 开始充电
     * @param userId 用户ID
     * @return 充电响应DTO
     */
    ChargingResponseDTO startCharging(Long userId);
    
    /**
     * 结束充电
     * @param userId 用户ID
     * @return 充电响应DTO
     */
    ChargingResponseDTO stopCharging(Long userId);
    
    /**
     * 自动停止充电（系统调用）
     * @param requestId 请求ID
     * @return 充电响应DTO
     */
    ChargingResponseDTO autoStopCharging(String requestId);
    
    /**
     * 取消充电请求
     * @param userId 用户ID
     * @return 充电响应DTO
     */
    ChargingResponseDTO cancelRequest(Long userId);
    
    /**
     * 获取充电状态
     * @param requestId 请求ID
     * @return 充电状态DTO
     */
    ChargingStatusDTO getChargingStatus(String requestId);
    
    /**
     * 获取用户活跃充电请求的状态
     * @param userId 用户ID
     * @return 充电状态DTO
     */
    ChargingStatusDTO getActiveChargingStatus(Long userId);
    
    /**
     * 根据请求ID获取充电请求
     * @param requestId 请求ID
     * @return 充电请求
     */
    ChargingRequest getRequestByRequestId(String requestId);
    
    /**
     * 获取用户活跃的充电请求
     * @param userId 用户ID
     * @return 活跃的充电请求，如果没有则返回null
     */
    ChargingRequest getActiveRequest(Long userId);
    
    /**
     * 获取请求前面的排队数量
     * @param requestId 请求ID
     * @return 前面排队的数量
     */
    int getQueueCountBefore(String requestId);
    
    /**
     * 获取充电桩的排队信息
     * @param pileId 充电桩ID
     * @return 排队车辆信息列表
     */
    List<Map<String, Object>> getPileQueueInfo(Integer pileId);
    
    /**
     * 生成请求ID
     * @param chargingMode 充电模式
     * @return 生成的请求ID
     */
    String generateRequestId(String chargingMode);
    
    /**
     * 充电后更新用户当前电量
     * @param request 充电请求
     */
    void updateUserCurrentPowerAfterCharging(ChargingRequest request);
} 