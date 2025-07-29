<template>
  <section class="bill-detail">
    <h3>充电详单详情</h3>
    
    <div v-if="loading" class="loading">
      <div class="loading-spinner"></div>
      <p>正在加载详单信息...</p>
    </div>
    
    <div v-else-if="error" class="error">
      <p>{{ error }}</p>
      <div class="error-actions">
        <button @click="fetchBillDetail" class="retry-btn">重试</button>
        <button @click="goBack" class="back-btn">返回列表</button>
      </div>
    </div>
    
    <div v-else-if="bill" class="bill-content">
      <div class="bill-section">
        <h4>基本信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <label>详单编号：</label>
            <span>{{ bill.billNumber }}</span>
          </div>
          <div class="info-item">
            <label>生成时间：</label>
            <span>{{ formatDateTime(bill.billTime) }}</span>
          </div>
          <div class="info-item">
            <label>充电请求ID：</label>
            <span>{{ bill.requestId }}</span>
          </div>
          <div class="info-item">
            <label>充电桩编号：</label>
            <span>{{ bill.pileNumber }}</span>
          </div>
        </div>
      </div>

      <div class="bill-section">
        <h4>充电信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <label>充电模式：</label>
            <span>{{ getChargingModeText(bill.chargingMode) }}</span>
          </div>
          <div class="info-item">
            <label>充电功率：</label>
            <span>{{ bill.chargingPower || 0 }} kW</span>
          </div>
          <div class="info-item">
            <label>充电电量：</label>
            <span>{{ bill.chargedAmount || 0 }} kWh</span>
          </div>
          <div class="info-item">
            <label>充电时长：</label>
            <span>{{ bill.chargingDuration || 0 }} 分钟</span>
          </div>
        </div>
      </div>

      <div class="bill-section">
        <h4>时间信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <label>启动时间：</label>
            <span>{{ formatDateTime(bill.startTime) }}</span>
          </div>
          <div class="info-item">
            <label>结束时间：</label>
            <span>{{ formatDateTime(bill.stopTime) }}</span>
          </div>
        </div>
      </div>

      <div class="bill-section">
        <h4>费用信息</h4>
        <div class="info-grid">
          <div class="info-item">
            <label>充电费用：</label>
            <span class="fee-amount">{{ formatAmount(bill.chargingFee) }} 元</span>
          </div>
          <div class="info-item">
            <label>服务费用：</label>
            <span class="fee-amount">{{ formatAmount(bill.serviceFee) }} 元</span>
          </div>
          <div class="info-item total-fee">
            <label>总费用：</label>
            <span class="total-amount">{{ formatAmount(bill.totalFee) }} 元</span>
          </div>
        </div>
      </div>
    </div>

    <div class="actions">
      <button @click="goBack" class="back-btn">← 返回列表</button>
      <button @click="printBill" class="print-btn">打印详单</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import apiService from '../utils/api'

const route = useRoute()
const router = useRouter()
const bill = ref(null)
const loading = ref(false)
const error = ref('')

const billId = computed(() => route.params.billId)

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 获取充电模式文本
const getChargingModeText = (mode) => {
  switch (mode) {
    case 'fast':
      return '快充'
    case 'slow':
      return '慢充'
    default:
      return mode || '-'
  }
}

// 格式化金额
const formatAmount = (amount) => {
  if (amount == null || amount === undefined) return '0.00'
  
  // 处理数字、字符串和 BigDecimal 类型
  let numAmount
  if (typeof amount === 'object' && amount !== null) {
    // 如果是对象类型（可能是 BigDecimal），尝试获取其值
    numAmount = amount.value || amount.amount || amount
  } else {
    numAmount = amount
  }
  
  // 转换为数字并格式化
  const num = parseFloat(numAmount)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

// 获取详单详情
const fetchBillDetail = async () => {
  if (!billId.value) {
    error.value = '缺少详单ID参数'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    // 确保 billId 是数字类型
    const id = parseInt(billId.value)
    if (isNaN(id)) {
      throw new Error('无效的详单ID')
    }
    
    const data = await apiService.bills.getBillById(id)
    if (!data) {
      throw new Error('未找到该详单信息')
    }
    bill.value = data
    
    // 只在开发环境输出日志
    if (process.env.NODE_ENV === 'development') {
      console.log('获取到的详单详情:', bill.value)
    }
  } catch (err) {
    error.value = err.message || '获取详单详情失败'
    console.error('获取详单详情失败:', err)
  } finally {
    loading.value = false
  }
}

// 返回详单列表
const goBack = () => {
  router.push('/user/bill/list')
}

// 打印详单
const printBill = () => {
  window.print()
}

onMounted(() => {
  fetchBillDetail()
})
</script>

<style scoped>
.bill-detail {
  background: #fff;
  padding: 1.5rem;
  max-width: 800px;
  margin: 0 auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

h3 {
  color: #409eff;
  border-bottom: 2px solid #409eff;
  padding-bottom: 0.5rem;
  margin-bottom: 1.5rem;
}

.loading, .error {
  text-align: center;
  padding: 2rem;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem auto;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error {
  color: #f56c6c;
}

.error-actions {
  margin-top: 1rem;
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.retry-btn {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.3s;
}

.retry-btn:hover {
  background-color: #66b1ff;
}

.bill-content {
  margin-bottom: 2rem;
}

.bill-section {
  margin-bottom: 1.5rem;
  padding: 1rem;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.bill-section h4 {
  color: #606266;
  margin-bottom: 1rem;
  font-size: 1.1rem;
  border-bottom: 1px solid #dcdfe6;
  padding-bottom: 0.5rem;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 0.5rem 0;
}

.info-item label {
  font-weight: bold;
  color: #606266;
  min-width: 100px;
  margin-right: 0.5rem;
}

.info-item span {
  color: #303133;
  flex: 1;
}

.fee-amount {
  color: #e6a23c;
  font-weight: bold;
}

.total-fee {
  grid-column: 1 / -1;
  border-top: 2px solid #409eff;
  padding-top: 1rem;
  margin-top: 1rem;
}

.total-amount {
  color: #f56c6c;
  font-weight: bold;
  font-size: 1.2rem;
}

.actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 2rem;
}

.back-btn, .print-btn {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.3s;
}

.back-btn {
  background-color: #67c23a;
  color: white;
}

.back-btn:hover {
  background-color: #85ce61;
}

.print-btn {
  background-color: #409eff;
  color: white;
}

.print-btn:hover {
  background-color: #66b1ff;
}

/* 打印样式 */
@media print {
  .actions {
    display: none;
  }
  
  .bill-detail {
    box-shadow: none;
    padding: 1rem;
  }
}
</style>
