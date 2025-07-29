package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.model.entity.ChargingPile;

import java.util.List;

public interface ChargingPileService extends IService<ChargingPile> {
    
    /**
     * 获取所有充电桩
     * @return 充电桩列表
     */
    List<ChargingPile> getAllPiles();
    
    /**
     * 根据类型获取充电桩
     * @param type 充电桩类型
     * @return 充电桩列表
     */
    List<ChargingPile> getPilesByType(String type);
    
    /**
     * 获取充电桩队列长度
     * @param pileId 充电桩ID
     * @return 队列长度
     */
    int getQueueLengthByPileId(Integer pileId);
    
    /**
     * 计算在指定充电桩充电所需的等待时间
     * @param pileId 充电桩ID
     * @return 等待时间（分钟）
     */
    double calculateWaitingTime(Integer pileId);
    
    /**
     * 根据充电模式和充电量，找到完成充电所需时间最短的充电桩
     * @param chargingMode 充电模式
     * @param chargingAmount 充电量
     * @return 最佳充电桩
     */
    ChargingPile findOptimalPile(String chargingMode, Double chargingAmount);
    
    /**
     * 根据充电桩类型获取可用的充电桩列表
     * @param pileType 充电桩类型（FAST或SLOW）
     * @return 可用充电桩列表
     */
    List<ChargingPile> getAvailablePilesByType(String pileType);
    
    /**
     * 切换充电桩状态
     * @param pileNumber 充电桩编号
     * @return 是否切换成功
     */
    boolean togglePileStatus(String pileNumber);
    
    /**
     * 添加新的充电桩
     * @param pileType 充电桩类型（FAST或SLOW）
     * @return 新创建的充电桩
     */
    ChargingPile addChargingPile(String pileType);
    
    /**
     * 删除充电桩
     * @param pileNumber 充电桩编号
     * @return 是否删除成功
     */
    boolean deleteChargingPile(String pileNumber);
} 