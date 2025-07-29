<template>
  <div class="pile-detail-page">
    <h2>充电桩 {{ pileId }} 详情</h2>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="!loading && !error">
      <div class="summary-box">
        <p><strong>是否正常工作：</strong> {{ isWorking ? '正常' : '关闭' }}</p>
        <p><strong>累计充电次数：</strong> {{ detail.totalCharges }}</p>
        <p><strong>累计充电时长：</strong> {{ detail.totalTime }} 分钟</p>
        <p><strong>累计充电电量：</strong> {{ detail.totalPower }} kWh</p>
      </div>

      <div class="queue-box">
        <h3>车辆服务信息</h3>
        <div v-if="queue.length === 0">暂无车辆信息</div>
        <div v-else>
          <div v-for="(item, index) in queue" :key="index" class="vehicle-item">
            <div class="vehicle-info">
              <div class="vehicle-header">
                <span class="position-badge">{{ item.queuePosition || '-' }}</span>
                <span class="status-badge" :class="getStatusClass(item.status)">
                  {{ getStatusText(item.status) }}
                </span>
              </div>
              <div class="vehicle-details">
                <p><strong>用户：</strong>{{ item.username }}</p>
                <p><strong>电池容量：</strong>{{ item.totalCapacity }} kWh</p>
                <p><strong>请求电量：</strong>{{ item.chargingAmount }} kWh</p>
                <p><strong>{{ item.timeLabel }}：</strong>{{ item.timeInfo }} 分钟</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="button-group">
        <button 
          :disabled="isWorking || toggleLoading" 
          @click="setStatus(true)" 
          class="open-btn">
          {{ toggleLoading ? '处理中...' : '开启充电桩' }}
        </button>
        <button 
          :disabled="!isWorking || toggleLoading" 
          @click="setStatus(false)" 
          class="close-btn">
          {{ toggleLoading ? '处理中...' : '关闭充电桩' }}
        </button>
        <button @click="goBack" class="back-btn">返回控制台</button>
        <button @click="goToList" class="back-btn">返回充电桩状态列表</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdmin } from '@/utils/useApi'

const route = useRoute()
const router = useRouter()
const pileId = route.params.pileId

const detail = ref({})
const isWorking = ref(false)
const queue = ref([])
const toggleLoading = ref(false)

// 使用统一的API
const { loading, error, getPileDetail, togglePileStatus } = useAdmin()

onMounted(async () => {
  await fetchPileDetail()
})

// 从后端获取充电桩的信息
async function fetchPileDetail() {
  try {
    const result = await getPileDetail(pileId)
    if (result) {
      // 获取是否正常工作，累计充电次数，累计充电时时长，累计充电电量
      detail.value = result.detail || {}
      // 获取初始开启，关闭状态
      isWorking.value = result.detail?.isWorking || false
      // 获取等待服务队列情况
      queue.value = result.queue || []
    }
  } catch (e) {
    console.error('❌ 获取充电桩详情失败', e)
  }
}

// 更改充电桩状态
async function setStatus(toOpen) {
  const current = isWorking.value;
  if (toOpen === current) return; // 如果已经是目标状态，不重复提交
  
  toggleLoading.value = true
  try {
    // 向后端发送切换请求（开启 / 关闭）
    const result = await togglePileStatus(pileId)
    
    if (result && result.success) {
      // 状态切换成功，刷新页面数据
      await fetchPileDetail()
    } else {
      alert(result?.message || '状态切换失败')
    }
  } catch (e) {
    console.error('❌ 切换充电桩状态失败', e)
    alert('状态切换失败')
  } finally {
    toggleLoading.value = false
  }
}

function goBack() {
  router.replace('/admin/console')
}

function goToList() {
  router.push('/admin/ChargingPileList')  // ⚠️ 路径请与 ChargingPileList.vue 页面在路由中的配置一致
}

// 获取状态样式类名
function getStatusClass(status) {
  switch (status) {
    case 'CHARGING':
      return 'status-charging'
    case 'WAITING':
      return 'status-waiting'
    default:
      return 'status-unknown'
  }
}

// 获取状态显示文本
function getStatusText(status) {
  switch (status) {
    case 'CHARGING':
      return '正在充电'
    case 'WAITING':
      return '排队等待'
    default:
      return '未知状态'
  }
}
</script>

<style scoped>
.pile-detail-page {
  padding: 2rem;
  font-family: Arial, sans-serif;
}

.loading {
  text-align: center;
  padding: 2rem;
  font-size: 1.1rem;
  color: #666;
}

.error-message {
  background-color: #ffebee;
  color: #c62828;
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  text-align: center;
}

.summary-box, .queue-box {
  margin-bottom: 2rem;
  padding: 1rem;
  border-radius: 8px;
  background: #f9f9f9;
  border: 1px solid #ccc;
}

.button-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.open-btn, .close-btn, .back-btn {
  padding: 0.6rem 1.2rem;
  font-weight: bold;
  border-radius: 6px;
  cursor: pointer;
  border: none;
  transition: all 0.2s ease;
}

.open-btn {
  background-color: #007bff;
  color: white;
}

.open-btn:disabled {
  background-color: #d0e5ff;
  color: #999;
  cursor: not-allowed;
}

.open-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.close-btn {
  background-color: red;
  color: white;
}

.close-btn:disabled {
  background-color: #f5bfbf;
  color: #999;
  cursor: not-allowed;
}

.close-btn:hover:not(:disabled) {
  background-color: darkred;
}

.back-btn {
  background-color: #ccc;
  color: #333;
}

.back-btn:hover {
  background-color: #aaa;
  color: white;
}

@media (max-width: 768px) {
  .button-group {
    flex-direction: column;
  }
  
  .open-btn, .close-btn, .back-btn {
    width: 100%;
  }
}

.vehicle-item {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  margin-bottom: 1rem;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.vehicle-info {
  padding: 1rem;
}

.vehicle-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.position-badge {
  background: #6c757d;
  color: white;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: bold;
  min-width: 24px;
  text-align: center;
}

.status-badge {
  padding: 0.3rem 0.8rem;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: bold;
  text-transform: uppercase;
}

.status-charging {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-waiting {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.status-unknown {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.vehicle-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.3rem;
}

.vehicle-details p {
  margin: 0;
  font-size: 0.9rem;
}

.vehicle-details strong {
  color: #555;
}
</style>
