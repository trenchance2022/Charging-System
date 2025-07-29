<template>
  <section class="bill-list">
    <h3>充电详单列表</h3>

    <div class="bill-header">
      <p>详单编号、生成时间、充电桩编号、总费用、充电电量、启动时间、充电时长</p>
    </div>

    <div v-if="bills.length === 0" class="no-bills">
      <p>暂无充电详单记录</p>
    </div>

    <table v-else class="bill-table">
      <thead>
      <tr>
        <th>详单编号</th>
        <th>生成时间</th>
        <th>充电桩编号</th>
        <th>总费用</th>
        <th>充电电量</th>
        <th>启动时间</th>
        <th>充电时长</th>
        <th>操作</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="bill in paginatedBills" :key="bill.id">
        <td>{{ bill.billNumber }}</td>
        <td>{{ formatDate(bill.billTime) }}</td>
        <td>{{ bill.pileNumber }}</td>
        <td>{{ formatAmount(bill.totalFee) }} 元</td>
        <td>{{ bill.chargedAmount }} kWh</td>
        <td>{{ formatDate(bill.startTime) }}</td>
        <td>{{ bill.chargingDuration }} 分钟</td>
        <td><button @click="viewDetail(bill.id)">查看</button></td>
      </tr>
      </tbody>
    </table>

    <div class="pagination">
      <button @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
      <button @click="nextPage" :disabled="currentPage >= totalPages">下一页</button>
    </div>

    <div class="back-button">
      <button @click="backToConsole">← 返回用户控制台</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import apiService from '../utils/api'

const router = useRouter()
const bills = ref([])
const currentPage = ref(1)
const pageSize = 10
const total = ref(0)
const loading = ref(false)
const error = ref('')

const totalPages = computed(() => Math.ceil(total.value / pageSize))

// 计算当前页显示的数据
const paginatedBills = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  const end = start + pageSize
  return bills.value.slice(start, end)
})

async function loadBills() {
  loading.value = true
  error.value = ''
  
  try {
    const data = await apiService.bills.getCurrentUserBills()
    bills.value = data || []
    total.value = bills.value.length
    
    // 只在开发环境输出日志
    if (process.env.NODE_ENV === 'development') {
      console.log('获取到的详单数据:', bills.value)
    }
  } catch (err) {
    error.value = err.message || '获取详单数据失败'
    console.error('获取详单失败:', err)
  } finally {
    loading.value = false
  }
}

// 查看详单详情
function viewDetail(billId) {
  router.push(`/user/bill/${billId}`)
}

// 分页控制
function goToPage(page) {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
  }
}

function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

// 格式化日期
function formatDate(dateString) {
  if (!dateString) return '-'
  return new Date(dateString).toLocaleString('zh-CN')
}

// 格式化金额
function formatAmount(amount) {
  if (amount == null) return '0.00'
  return Number(amount).toFixed(2)
}

// 返回控制台
function backToConsole() {
  router.push('/user/console')
}

onMounted(() => {
  loadBills()
})
</script>

<style scoped>
.bill-list {
  padding: 1rem;
  background: #fff;
  max-width: 1200px;
  margin: 0 auto;
}

.bill-header {
  margin-bottom: 1rem;
  background: #f0f0f0;
  padding: 0.6rem;
  border-radius: 6px;
  text-align: center;
  font-weight: bold;
}

.no-bills {
  text-align: center;
  padding: 2rem;
  color: #666;
  font-size: 1.1rem;
}

.bill-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 1rem;
}

.bill-table th,
.bill-table td {
  border: 1px solid #ddd;
  padding: 0.5rem 0.8rem;
  text-align: center;
  font-size: 0.9rem;
}

.bill-table th {
  background: #f7f7f7;
  font-weight: bold;
}

.bill-table tr:nth-child(even) {
  background: #f9f9f9;
}

.bill-table tr:hover {
  background: #f0f8ff;
}

.bill-table button {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 0.3rem 0.6rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
}

.bill-table button:hover {
  background-color: #66b1ff;
}

.pagination {
  margin-top: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1rem;
}

.pagination button {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.back-button {
  margin-top: 1rem;
  text-align: center;
}

.back-button button {
  background-color: #67c23a;
  color: white;
  border: none;
  padding: 0.4rem 0.8rem;
  border-radius: 4px;
  font-size: 0.95rem;
  cursor: pointer;
}

.back-button button:hover {
  background-color: #85ce61;
}
</style>