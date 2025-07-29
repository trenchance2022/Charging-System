<template>
  <section>
    <h3>充电状态</h3>
    <p>
      状态：
      <span
          :class="{
          charging: store.chargeStatus === 'CHARGING',
          notCharging: store.chargeStatus !== 'CHARGING'
        }"
      >
        {{
          store.chargeStatus === 'CHARGING' ? '充电中' : '未充电'
        }}
      </span>
    </p>
    
    <!-- 显示电池信息 -->
    <p>
      当前电量/电池总量: {{ (store.currentPower || 0).toFixed(2) }} / {{ displayBatteryCapacity }} kWh
    </p>
    
    <!-- 显示电池容量设置提示（如果没有设置电池容量） -->
    <p v-if="!store.batteryCapacity" class="warning-text">
      ⚠️ 请先设置电池容量才能进行充电
    </p>
    
    <!-- 显示充电进度信息（仅当有活跃充电请求时） -->
    <p v-if="store.hasActiveRequest">
      已充电量/请求充电量: {{ (store.chargedAmount || 0).toFixed(2) }} / {{ store.requestedAmount || 0 }} kWh
    </p>
    
    <!-- 显示充电进度条（仅当正在充电时） -->
    <div v-if="store.chargeStatus === 'CHARGING'" class="progress-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: chargingProgress + '%' }"></div>
      </div>
      <span class="progress-text">{{ chargingProgress.toFixed(1) }}%</span>
    </div>

    <!-- 只有正在充电时显示剩余时间 -->
    <p v-if="store.chargeStatus === 'CHARGING'">
      预计剩余充电时间: {{ store.remainingTime || 0 }} 分钟
    </p>
    
    <!-- 实时计费信息 -->
    <div v-if="store.hasActiveRequest" class="billing-section">
      <h4>实时计费</h4>
      
      <!-- 当前已产生费用（充电中或已完成时显示） -->
      <div v-if="store.chargeStatus === 'CHARGING' || store.chargeStatus === 'COMPLETED'" class="current-billing">
        <p class="billing-item billing-total">
          <span class="billing-label">当前总费用:</span>
          <span class="billing-value total">{{ formatCurrency(store.currentTotalFee) }} 元</span>
        </p>
      </div>
      
      <!-- 预计总费用（等待中或充电中时显示） -->
      <div v-if="store.chargeStatus === 'WAITING' || store.chargeStatus === 'CHARGING'" class="estimated-billing">
        <p class="billing-item billing-total" :class="{ 'with-separator': store.chargeStatus === 'CHARGING' }">
          <span class="billing-label">预计总费用:</span>
          <span class="billing-value total estimated">{{ formatCurrency(store.estimatedTotalFee) }} 元</span>
        </p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onBeforeUnmount, computed } from 'vue'
import { useChargeStore } from '../stores/chargeStatus'

const store = useChargeStore()

let eventSource = null

// 显示的电池容量（优先使用用户设置的电池容量）
const displayBatteryCapacity = computed(() => {
  return store.batteryCapacity || store.totalCapacity || '未设置'
})

// 计算充电进度百分比
const chargingProgress = computed(() => {
  if (store.requestedAmount > 0) {
    return Math.min(((store.chargedAmount || 0) / store.requestedAmount) * 100, 100)
  }
  return 0
})

// 格式化货币显示
const formatCurrency = (amount) => {
  if (amount === null || amount === undefined) {
    return '0.00'
  }
  return Number(amount).toFixed(2)
}

onMounted(async () => {
  // 初始化时获取用户电池容量和充电状态
  try {
    await store.fetchBatteryCapacity()
    await store.fetchChargeStatus()
    console.log('ChargeStatus: 初始化完成', {
      batteryCapacity: store.batteryCapacity,
      totalCapacity: store.totalCapacity,
      currentPower: store.currentPower
    })
  } catch (error) {
    console.error('初始化充电状态失败:', error)
  }
  
  connectSSE()
})

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})

// 建立SSE连接
const connectSSE = () => {
  // 关闭之前的连接
  if (eventSource) {
    eventSource.close()
  }
  
  // 获取JWT令牌
  const token = localStorage.getItem('jwt')
  if (!token) {
    console.error('未找到JWT令牌，无法建立SSE连接')
    return
  }
  
  // 创建新的SSE连接，通过URL参数传递JWT令牌
  eventSource = new EventSource(`/api/charge/status/stream/user?token=${token}`)
  
  // 监听消息
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      
      // 添加调试信息：输出接收到的数据
      console.log('接收到充电状态数据:', data)
      console.log('充电状态详细信息:', {
        status: data.status,
        currentPower: data.currentPower,
        chargedAmount: data.chargedAmount,
        totalCapacity: data.totalCapacity,
        requestedAmount: data.requestedAmount,
        remainingTime: data.remainingTime,
        isQueueFirst: data.isQueueFirst,
        chargingPileId: data.chargingPileId,
        isAutoCompleted: data.isAutoCompleted,
        chargingPileStatus: data.chargingPileStatus,
        // 计费信息
        currentTotalFee: data.currentTotalFee,
        estimatedTotalFee: data.estimatedTotalFee
      })
      
      // 更新store中的数据
      store.updateChargeStatus(data)
      
      
      // 检查是否为自动完成：状态变为COMPLETED且后端标记为自动完成
      if (data.status === 'COMPLETED' && data.isAutoCompleted === true) {
        console.log('检测到自动完成充电')
        // 使用store的自动完成方法
        store.handleAutoComplete()
        
        // 显示自动完成提示
        alert('充电已自动完成！请查看充电详单。')
      }
    } catch (err) {
      console.error('解析充电状态失败', err)
    }
  }
  
  // 错误处理
  eventSource.onerror = (err) => {
    console.error('SSE连接错误', err)
    eventSource.close()
    
    // 3秒后尝试重新连接
    setTimeout(connectSSE, 3000)
  }
}
</script>

<style scoped>
section {
  background: #f9fafc;
  padding: 1.2rem 1.8rem;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgb(0 0 0 / 0.1);
  margin-bottom: 1rem;
  transition: box-shadow 0.3s ease;
}
section:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 24px rgb(0 0 0 / 0.15);
}

h3 {
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #409eff;
  border-bottom: 2px solid #409eff;
  padding-bottom: 0.3rem;
}

p {
  margin: 0.4rem 0;
  font-size: 1rem;
  line-height: 1.5;
  color: #606266;
}

.charging {
  color: #67c23a;
  font-weight: bold;
}

.notCharging {
  color: #f56c6c;
  font-weight: bold;
}

.warning-text {
  color: #e6a23c;
  font-weight: 500;
  background: #fdf6ec;
  padding: 0.5rem;
  border-radius: 4px;
  border-left: 4px solid #e6a23c;
}

.progress-container {
  margin: 0.8rem 0;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background-color: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #67c23a;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.9rem;
  font-weight: 500;
  color: #67c23a;
  min-width: 50px;
  text-align: right;
}

.billing-section {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgb(0 0 0 / 0.1);
  border-left: 4px solid #409eff;
}

.billing-section h4 {
  margin-bottom: 0.8rem;
  font-weight: 600;
  color: #409eff;
  border-bottom: 2px solid #409eff;
  padding-bottom: 0.3rem;
  font-size: 1rem;
}

.current-billing {
  margin-bottom: 0.5rem;
}

.estimated-billing {
  margin-top: 0.5rem;
}

.billing-item {
  margin: 0.6rem 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #606266;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.billing-label {
  font-weight: 500;
  font-size: 0.95rem;
}

.billing-value {
  font-weight: 600;
}

.billing-value.total {
  color: #e6a23c;
  font-size: 1rem;
  font-weight: 700;
}

.billing-value.estimated {
  color: #909399;
}

.billing-total {
  font-weight: bold;
  background-color: #f8f9fa;
  padding: 0.8rem;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  margin: 0.8rem 0;
}

.billing-total.with-separator {
  margin-top: 1.2rem;
  border-top: 2px solid #e4e7ed;
  padding-top: 1rem;
}
</style>
