// src/stores/chargeStatus.js
// 统一的充电系统状态管理
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiService from '../utils/api'

export const useChargeStore = defineStore('charge', () => {
    // ======================== 充电状态相关 ========================
    const chargeStatus = ref('NOT_CHARGING')  // 充电状态
    const currentPower = ref(0)               // 当前电量（电池当前储电量）
    const chargedAmount = ref(0)              // 已充电量（本次充电请求的已充电量）
    const totalCapacity = ref(0)              // 电池总量
    const requestedAmount = ref(0)            // 请求充电量
    const remainingTime = ref(0)              // 剩余充电时间
    const chargingPileId = ref(null)          // 充电桩ID
    const isQueueFirst = ref(false)           // 是否在队首
    const chargingPileStatus = ref(null)      // 充电桩状态
    
    // ======================== 实时计费相关 ========================
    const currentTotalFee = ref(0)            // 当前已产生的总费用（元）
    const estimatedTotalFee = ref(0)          // 预计总费用（元）
    
    // ======================== 电池容量相关 ========================
    const batteryCapacity = ref(null)         // 用户电池容量（kWh）
    
    // ======================== 队列状态相关 ========================
    const queueNumber = ref(0)                // 排队号码
    const queueCount = ref(0)                 // 前方车辆数
    const estimatedWait = ref(0)              // 预计等待时间
    const locationType = ref('NONE')          // 位置类型: NONE, WAITING_AREA, CHARGING_PILE
    
    // ======================== 价格信息相关 ========================
    const currentPeriod = ref('加载中...')    // 当前时段
    const unitPrice = ref(0)                  // 单位电价
    const serviceFeeRate = ref(0)             // 服务费率
    const priceType = ref('')                 // 价格类型
    
    // ======================== 通知管理相关 ========================
    const notifications = ref([])             // 通知列表
    let notificationIdCounter = 0             // 通知ID计数器
    
    // ======================== 计算属性 ========================
    const totalPricePerUnit = computed(() => {
        return (unitPrice.value + serviceFeeRate.value).toFixed(2)
    })
    
    const hasActiveRequest = computed(() => {
        return chargeStatus.value === 'WAITING' || chargeStatus.value === 'PRIORITY_WAITING' || chargeStatus.value === 'CHARGING'
    })
    
    const isInChargingArea = computed(() => {
        return chargeStatus.value === 'CHARGING' || 
               (chargeStatus.value === 'WAITING' && chargingPileId.value !== null)
    })
    
    const canStartCharge = computed(() => {
        return chargeStatus.value === 'WAITING' && isQueueFirst.value && chargingPileStatus.value === 'AVAILABLE'
    })
    
    const canStopCharge = computed(() => {
        return chargeStatus.value === 'CHARGING'
    })
    
    const canCancelCharge = computed(() => {
        return chargeStatus.value === 'WAITING' || chargeStatus.value === 'PRIORITY_WAITING'
    })
    
    // 检查是否可以提交充电请求（必须先设置电池容量）
    const canSubmitRequest = computed(() => {
        return batteryCapacity.value !== null && batteryCapacity.value > 0
    })
    
    // 检查是否可以设置电池容量（无活跃充电请求时可设置）
    const canSetBatteryCapacity = computed(() => {
        return !hasActiveRequest.value
    })
    
    // ======================== 通知管理方法 ========================
    function addNotification(notification) {
        // 添加唯一ID和时间戳
        notification.id = ++notificationIdCounter
        notification.timestamp = Date.now()
        
        // 添加到通知列表
        notifications.value.unshift(notification)
        
        // 限制通知数量，最多保留10条
        if (notifications.value.length > 10) {
            notifications.value = notifications.value.slice(0, 10)
        }
        
        // 5秒后自动移除info级别的通知
        if (notification.level === 'INFO') {
            setTimeout(() => {
                removeNotification(notification.id)
            }, 5000)
        }
    }
    
    function removeNotification(notificationId) {
        const index = notifications.value.findIndex(n => n.id === notificationId)
        if (index !== -1) {
            notifications.value.splice(index, 1)
        }
    }
    
    function clearAllNotifications() {
        notifications.value = []
    }
    
    // ======================== 充电状态方法 ========================
    function startCharging() {
        chargeStatus.value = 'CHARGING'
    }

    function stopCharging() {
        chargeStatus.value = 'COMPLETED'
        // 清空相关状态
        chargingPileId.value = null
        isQueueFirst.value = false
        chargingPileStatus.value = null
        queueNumber.value = 0
        queueCount.value = 0
        estimatedWait.value = 0
        locationType.value = 'NONE'
    }

    function resetCharging() {
        chargeStatus.value = 'NOT_CHARGING'
        // 清空所有状态
        currentPower.value = 0
        totalCapacity.value = 0
        remainingTime.value = 0
        chargingPileId.value = null
        isQueueFirst.value = false
        chargingPileStatus.value = null
        queueNumber.value = 0
        queueCount.value = 0
        estimatedWait.value = 0
        locationType.value = 'NONE'
    }
    
    function setWaiting() {
        chargeStatus.value = 'WAITING'
    }
    
    function setCanceled() {
        chargeStatus.value = 'CANCELED'
        // 清空相关状态
        chargingPileId.value = null
        isQueueFirst.value = false
        chargingPileStatus.value = null
        queueNumber.value = 0
        queueCount.value = 0
        estimatedWait.value = 0
        locationType.value = 'NONE'
    }
    
    function handleAutoComplete() {
        chargeStatus.value = 'COMPLETED'
    }
    
    // ======================== 状态更新方法 ========================
    function updateChargeStatus(data) {
        if (data.status) chargeStatus.value = data.status
        if (data.currentPower !== undefined) currentPower.value = data.currentPower
        if (data.chargedAmount !== undefined) chargedAmount.value = data.chargedAmount
        if (data.totalCapacity !== undefined) totalCapacity.value = data.totalCapacity
        if (data.requestedAmount !== undefined) requestedAmount.value = data.requestedAmount
        if (data.remainingTime !== undefined) remainingTime.value = data.remainingTime
        if (data.chargingPileId !== undefined) chargingPileId.value = data.chargingPileId
        if (data.isQueueFirst !== undefined) isQueueFirst.value = data.isQueueFirst
        if (data.chargingPileStatus !== undefined) chargingPileStatus.value = data.chargingPileStatus
        
        // 更新计费信息
        if (data.currentTotalFee !== undefined) currentTotalFee.value = data.currentTotalFee
        if (data.estimatedTotalFee !== undefined) estimatedTotalFee.value = data.estimatedTotalFee
    }
    
    function updateQueueInfo(data) {
        if (data.queueNumber !== undefined) queueNumber.value = data.queueNumber
        if (data.queueCount !== undefined) queueCount.value = data.queueCount
        if (data.estimatedWait !== undefined) estimatedWait.value = data.estimatedWait
        if (data.locationType) locationType.value = data.locationType
    }
    
    function updatePricingInfo(data) {
        if (data.currentPeriod) currentPeriod.value = data.currentPeriod
        if (data.unitPrice !== undefined) unitPrice.value = data.unitPrice
        if (data.serviceFeeRate !== undefined) serviceFeeRate.value = data.serviceFeeRate
        if (data.priceType) priceType.value = data.priceType
    }
    
    // ======================== 数据获取方法 ========================
    async function fetchChargeStatus() {
        try {
            const data = await apiService.charge.getUserStatus()
            updateChargeStatus(data)
            return data
        } catch (error) {
            console.error('获取充电状态失败:', error)
            throw error
        }
    }
    
    async function fetchQueueStatus() {
        try {
            const data = await apiService.queue.getUserStatus()
            updateQueueInfo(data)
            return data
        } catch (error) {
            console.error('获取队列状态失败:', error)
            throw error
        }
    }
    
    async function fetchPricingInfo() {
        try {
            const data = await apiService.pricing.getCurrent()
            updatePricingInfo(data)
            return data
        } catch (error) {
            console.error('获取价格信息失败:', error)
            throw error
        }
    }
    
    // 获取用户电池容量
    async function fetchBatteryCapacity() {
        try {
            const data = await apiService.charge.getBatteryCapacity()
            batteryCapacity.value = data.batteryCapacity
            console.log('获取电池容量成功:', data.batteryCapacity)
            return data
        } catch (error) {
            console.error('获取电池容量失败:', error)
            // 设置默认值，避免显示异常
            batteryCapacity.value = null
            throw error
        }
    }
    
    // 设置用户电池容量
    async function setBatteryCapacity(capacity) {
        try {
            const data = await apiService.charge.setBatteryCapacity(capacity)
            if (data.success) {
                batteryCapacity.value = capacity
            }
            return data
        } catch (error) {
            console.error('设置电池容量失败:', error)
            throw error
        }
    }
    
    // 刷新所有状态
    async function refreshAllStatus() {
        try {
            await Promise.all([
                fetchChargeStatus(),
                fetchQueueStatus(),
                fetchPricingInfo(),
                fetchBatteryCapacity()
            ])
        } catch (error) {
            console.error('刷新状态失败:', error)
        }
    }
    
    return {
        // 状态
        chargeStatus,
        currentPower,
        totalCapacity,
        remainingTime,
        chargingPileId,
        isQueueFirst,
        chargingPileStatus,
        batteryCapacity,
        queueNumber,
        queueCount,
        estimatedWait,
        locationType,
        currentPeriod,
        unitPrice,
        serviceFeeRate,
        priceType,
        chargedAmount,
        requestedAmount,
        
        // 计费状态
        currentTotalFee,
        estimatedTotalFee,
        
        // 计算属性
        totalPricePerUnit,
        hasActiveRequest,
        isInChargingArea,
        canStartCharge,
        canStopCharge,
        canCancelCharge,
        canSubmitRequest,
        canSetBatteryCapacity,
        
        // 方法
        startCharging,
        stopCharging,
        resetCharging,
        setWaiting,
        setCanceled,
        handleAutoComplete,
        updateChargeStatus,
        updateQueueInfo,
        updatePricingInfo,
        fetchChargeStatus,
        fetchQueueStatus,
        fetchPricingInfo,
        fetchBatteryCapacity,
        setBatteryCapacity,
        refreshAllStatus,
        
        // 通知管理
        notifications,
        addNotification,
        removeNotification,
        clearAllNotifications
    }
})
