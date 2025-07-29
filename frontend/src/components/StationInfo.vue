<template>
  <section class="station-info">
    <h3>充电站信息</h3>
    <p>当前时段: {{ store.currentPeriod }}</p>
    <p>电费单价: ￥{{ store.unitPrice }}/kWh</p>
    <p>服务费单价: ￥{{ store.serviceFeeRate }}/kWh</p>
    <p>总计单价: ￥{{ store.totalPricePerUnit }}/kWh</p>
    <p class="price-tip" :class="priceTypeClass">{{ priceTip }}</p>
  </section>
</template>

<script setup>
import { computed, onMounted, onBeforeUnmount } from 'vue'
import { useChargeStore } from '../stores/chargeStatus'

const store = useChargeStore()

let eventSource = null

// 根据时段类型计算提示文字和样式
const priceTip = computed(() => {
  switch (store.priceType) {
    case 'PEAK':
      return '当前为峰时段，电价较高'
    case 'NORMAL':
      return '当前为平时段，电价适中'
    case 'VALLEY':
      return '当前为谷时段，电价最低'
    default:
      return ''
  }
})

const priceTypeClass = computed(() => {
  switch (store.priceType) {
    case 'PEAK':
      return 'peak-price'
    case 'NORMAL':
      return 'normal-price'
    case 'VALLEY':
      return 'valley-price'
    default:
      return ''
  }
})

onMounted(() => {
  connectSSE()
})

onBeforeUnmount(() => {
  // 组件卸载时关闭SSE连接
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
  eventSource = new EventSource(`/api/pricing/stream?token=${token}`)
  
  // 监听消息
  eventSource.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      store.updatePricingInfo(data)
    } catch (err) {
      console.error('解析电价信息失败', err)
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
.station-info {
  background: #f9fafc;
  padding: 1.2rem 1.8rem;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgb(0 0 0 / 0.1);
  margin-bottom: 1rem;
  transition: box-shadow 0.3s ease;
}
.station-info:hover {
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

.price-tip {
  margin-top: 0.8rem;
  font-weight: bold;
}

.peak-price {
  color: #f56c6c;
}

.normal-price {
  color: #e6a23c;
}

.valley-price {
  color: #67c23a;
}
</style>
