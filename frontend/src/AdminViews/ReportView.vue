<template>
  <div class="report-page">
    <h2>充电桩运营报表</h2>

    <!-- 查询条件 -->
    <div class="query-form">
      <div class="form-row">
        <div class="form-group">
          <label for="startDate">开始日期：</label>
          <input 
            id="startDate"
            type="date" 
            v-model="startDate" 
            class="date-input"
          />
        </div>
        <div class="form-group">
          <label for="endDate">结束日期：</label>
          <input 
            id="endDate"
            type="date" 
            v-model="endDate" 
            class="date-input"
          />
        </div>
        <div class="form-group">
          <button 
            @click="fetchReport" 
            :disabled="loading || !startDate || !endDate"
            class="query-btn"
          >
            {{ loading ? '查询中...' : '查询报表' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 错误信息 -->
    <div v-if="error" class="error-message">
      {{ error }}
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading">正在生成报表...</div>

    <!-- 报表数据 -->
    <div v-if="!loading && !error && reportData.length > 0" class="report-content">
      <div class="report-summary">
        <h3>统计期间：{{ startDate }} 至 {{ endDate }}</h3>
        <p>共有 {{ reportData.length }} 个充电桩产生数据</p>
      </div>

      <div class="report-table-container">
        <table class="report-table">
          <thead>
            <tr>
              <th>充电桩编号</th>
              <th>累计充电次数</th>
              <th>累计充电时长<br/>(分钟)</th>
              <th>累计充电量<br/>(kWh)</th>
              <th>累计充电费用<br/>(元)</th>
              <th>累计服务费用<br/>(元)</th>
              <th>累计总费用<br/>(元)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="pile in reportData" :key="pile.pileNumber">
              <td class="pile-number">{{ pile.pileNumber }}</td>
              <td class="number-cell">{{ pile.totalCharges }}</td>
              <td class="number-cell">{{ pile.totalTime }}</td>
              <td class="number-cell">{{ pile.totalPower }}</td>
              <td class="money-cell">{{ pile.totalChargingFee }}</td>
              <td class="money-cell">{{ pile.totalServiceFee }}</td>
              <td class="money-cell total-fee">{{ pile.totalFee }}</td>
            </tr>
          </tbody>
          <tfoot v-if="reportData.length > 1">
            <tr class="summary-row">
              <td><strong>合计</strong></td>
              <td class="number-cell"><strong>{{ totalSummary.totalCharges }}</strong></td>
              <td class="number-cell"><strong>{{ totalSummary.totalTime }}</strong></td>
              <td class="number-cell"><strong>{{ totalSummary.totalPower }}</strong></td>
              <td class="money-cell"><strong>{{ totalSummary.totalChargingFee }}</strong></td>
              <td class="money-cell"><strong>{{ totalSummary.totalServiceFee }}</strong></td>
              <td class="money-cell total-fee"><strong>{{ totalSummary.totalFee }}</strong></td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>

    <!-- 无数据提示 -->
    <div v-if="!loading && !error && reportData.length === 0 && hasQueried" class="no-data">
      <p>所选时间段内没有充电记录</p>
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <button @click="exportReport" :disabled="loading || reportData.length === 0" class="export-btn">
        导出报表
      </button>
      <button @click="goBack" class="back-btn">返回控制台</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdmin } from '@/utils/useApi'

const router = useRouter()
const reportData = ref([])
const hasQueried = ref(false)

// 初始化日期（默认最近7天）
const today = new Date().toISOString().split('T')[0]
const lastWeek = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]
const startDate = ref(lastWeek)
const endDate = ref(today)

// 使用统一的API
const { loading, error, getReportStatistics } = useAdmin()

// 计算总计数据
const totalSummary = computed(() => {
  if (reportData.value.length === 0) {
    return {
      totalCharges: 0,
      totalTime: 0,
      totalPower: 0,
      totalChargingFee: 0,
      totalServiceFee: 0,
      totalFee: 0
    }
  }

  return reportData.value.reduce((acc, pile) => {
    acc.totalCharges += pile.totalCharges || 0
    acc.totalTime += pile.totalTime || 0
    acc.totalPower += pile.totalPower || 0
    acc.totalChargingFee += parseFloat(pile.totalChargingFee || 0)
    acc.totalServiceFee += parseFloat(pile.totalServiceFee || 0)
    acc.totalFee += parseFloat(pile.totalFee || 0)
    return acc
  }, {
    totalCharges: 0,
    totalTime: 0,
    totalPower: 0,
    totalChargingFee: 0,
    totalServiceFee: 0,
    totalFee: 0
  })
})

onMounted(() => {
  // 页面加载时可以选择是否自动查询
})

// 获取报表数据
async function fetchReport() {
  if (!startDate.value || !endDate.value) {
    alert('请选择开始日期和结束日期')
    return
  }

  if (new Date(startDate.value) > new Date(endDate.value)) {
    alert('开始日期不能晚于结束日期')
    return
  }

  try {
    const result = await getReportStatistics(startDate.value, endDate.value)
    if (result) {
      reportData.value = result
      hasQueried.value = true
      console.log('✅ 报表数据:', reportData.value)
    }
  } catch (e) {
    console.error('❌ 获取报表数据失败', e)
  }
}

// 导出报表 (简单实现)
function exportReport() {
  if (reportData.value.length === 0) {
    alert('没有数据可导出')
    return
  }

  // 生成 CSV 内容
  let csvContent = "充电桩编号,累计充电次数,累计充电时长(分钟),累计充电量(kWh),累计充电费用(元),累计服务费用(元),累计总费用(元)\n"
  
  reportData.value.forEach(pile => {
    csvContent += `${pile.pileNumber},${pile.totalCharges},${pile.totalTime},${pile.totalPower},${pile.totalChargingFee},${pile.totalServiceFee},${pile.totalFee}\n`
  })

  // 添加合计行
  const summary = totalSummary.value
  csvContent += `合计,${summary.totalCharges},${summary.totalTime},${summary.totalPower},${summary.totalChargingFee.toFixed(2)},${summary.totalServiceFee.toFixed(2)},${summary.totalFee.toFixed(2)}\n`

  // 创建下载链接
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  
  if (link.download !== undefined) {
    const url = URL.createObjectURL(blob)
    link.setAttribute('href', url)
    link.setAttribute('download', `充电桩运营报表_${startDate.value}_${endDate.value}.csv`)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

function goBack() {
  router.push('/admin/console')
}
</script>

<style scoped>
.report-page {
  padding: 2rem;
  font-family: Arial, sans-serif;
  max-width: 1400px;
  margin: 0 auto;
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
}

.query-form {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 8px;
  margin-bottom: 2rem;
  border: 1px solid #e9ecef;
}

.form-row {
  display: flex;
  align-items: end;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 600;
  color: #495057;
  font-size: 0.9rem;
}

.date-input {
  padding: 0.6rem 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  min-width: 150px;
  transition: border-color 0.2s ease;
}

.date-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.query-btn {
  padding: 0.6rem 1.5rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  font-size: 0.9rem;
  transition: all 0.2s ease;
  min-width: 120px;
}

.query-btn:hover:not(:disabled) {
  background-color: #0056b3;
  transform: translateY(-1px);
}

.query-btn:disabled {
  background-color: #d0e5ff;
  color: #999;
  cursor: not-allowed;
  transform: none;
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

.no-data {
  text-align: center;
  padding: 3rem;
  color: #666;
  font-size: 1.1rem;
}

.report-content {
  margin-bottom: 2rem;
}

.report-summary {
  background: #e3f2fd;
  padding: 1rem 1.5rem;
  border-radius: 6px;
  margin-bottom: 1.5rem;
  border-left: 4px solid #2196f3;
}

.report-summary h3 {
  margin: 0 0 0.5rem 0;
  color: #1976d2;
  font-size: 1.1rem;
}

.report-summary p {
  margin: 0;
  color: #424242;
  font-size: 0.9rem;
}

.report-table-container {
  overflow-x: auto;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.report-table th,
.report-table td {
  padding: 0.75rem;
  text-align: center;
  border: 1px solid #e0e0e0;
}

.report-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: #333;
  font-size: 0.9rem;
  line-height: 1.2;
}

.report-table tbody tr:nth-child(even) {
  background-color: #fafafa;
}

.report-table tbody tr:hover {
  background-color: #f0f8ff;
}

.pile-number {
  font-weight: 600;
  color: #007bff;
}

.number-cell {
  font-family: 'Courier New', monospace;
  font-weight: 500;
}

.money-cell {
  font-family: 'Courier New', monospace;
  font-weight: 500;
  color: #2e7d32;
}

.total-fee {
  font-weight: 600;
  color: #d32f2f;
}

.summary-row {
  background-color: #e8f5e8 !important;
}

.summary-row td {
  border-top: 2px solid #4caf50;
  font-size: 0.95rem;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 2rem;
}

.export-btn {
  padding: 0.75rem 2rem;
  background-color: #52c41a;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.export-btn:hover:not(:disabled) {
  background-color: #389e0d;
  transform: translateY(-1px);
}

.export-btn:disabled {
  background-color: #d9f7be;
  color: #999;
  cursor: not-allowed;
  transform: none;
}

.back-btn {
  padding: 0.75rem 2rem;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.back-btn:hover {
  background-color: #545b62;
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .report-page {
    padding: 1rem;
  }
  
  .form-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .form-group {
    width: 100%;
  }
  
  .date-input {
    min-width: auto;
  }
  
  .report-table {
    font-size: 0.85rem;
  }
  
  .report-table th,
  .report-table td {
    padding: 0.5rem 0.25rem;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .export-btn,
  .back-btn {
    width: 100%;
  }
}
</style> 