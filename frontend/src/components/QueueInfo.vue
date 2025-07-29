<template>
  <section>
    <h3>排队信息</h3>
    <div v-if="store.locationType === 'NONE'">
      <p class="status-text not-queuing">未排队</p>
      <p>请点击"提交充电请求"按钮开始排队</p>
    </div>
    <div v-else-if="store.locationType === 'WAITING_AREA'">
      <p class="status-text waiting">等候区等待</p>
      <p>排队号码: <span class="highlight">{{ store.queueNumber }}</span></p>
      <p>前方车辆: <span class="highlight">{{ store.queueCount }}</span> 辆</p>
      <p v-if="store.queueCount === 0" class="status-text ready">已就绪，等待分配充电桩</p>
      <p v-else>预计等待时间: <span class="highlight">{{ store.estimatedWait }}</span> 分钟</p>
    </div>
    <div v-else-if="store.locationType === 'CHARGING_PILE'">
      <p class="status-text charging-queue">充电桩队列</p>
      <p>排队号码: <span class="highlight">{{ store.queueNumber }}</span></p>
      <p>前方车辆: <span class="highlight">{{ store.queueCount }}</span> 辆</p>
      <p v-if="store.queueCount === 0" class="status-text ready">已就绪，可以开始充电</p>
      <p v-else>预计等待时间: <span class="highlight">{{ store.estimatedWait }}</span> 分钟</p>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { useChargeStore } from '../stores/chargeStatus'

const store = useChargeStore()

let eventSource = null

onMounted(() => {
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
  eventSource = new EventSource(`/api/queue/status/stream/user?token=${token}`)
  
  // 监听消息
  eventSource.onmessage = (event) => {
    try {
      console.log(event.data)
      const data = JSON.parse(event.data)
      store.updateQueueInfo(data)
    } catch (err) {
      console.error('解析排队状态失败', err)
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
  transition: all 0.3s ease;
}

section:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgb(0 0 0 / 0.15);
}

h3 {
  margin-bottom: 1rem;
  font-weight: 600;
  color: #409eff;
  border-bottom: 2px solid #409eff;
  padding-bottom: 0.3rem;
}

p {
  margin: 0.6rem 0;
  font-size: 1rem;
  line-height: 1.5;
  color: #606266;
}

.status-text {
  font-weight: bold;
  font-size: 1.1rem;
}

.not-queuing {
  color: #f56c6c;
}

.waiting {
  color: #e6a23c;
}

.charging-queue {
  color: #409eff;
}

.ready {
  color: #67c23a;
}

.highlight {
  color: #409eff;
  font-weight: bold;
}
</style>
