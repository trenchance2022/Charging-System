<template>
  <div class="pile-list">
    <h2>充电桩状态列表</h2>

    <!-- 操作工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <button @click="showAddDialog = true" class="add-btn">
          ➕ 添加充电桩
        </button>
      </div>
      <div class="toolbar-right">
        <button @click="refreshData" :disabled="loading" class="refresh-btn">
          {{ loading ? '刷新中...' : '🔄 刷新数据' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="!loading && !error" class="pile-sections">
      <!-- 快充桩区域 -->
      <div class="pile-section">
        <h3 class="section-title">
          <span class="section-icon">⚡</span>
          快充桩（{{ fastPiles.length }}个）
        </h3>
        <div v-if="fastPiles.length === 0" class="no-piles">
          暂无快充桩
        </div>
        <table v-else class="pile-table">
          <thead>
          <tr>
            <th>充电桩编号</th>
            <th>是否正常工作</th>
            <th>累计充电次数</th>
            <th>累计充电时长（分钟）</th>
            <th>累计充电电量（kWh）</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="pile in fastPiles" :key="pile.id">
            <td class="pile-number fast-pile">{{ pile.id }}</td>
            <td :class="pile.isWorking ? 'working' : 'not-working'">
              {{ pile.isWorking ? '正常' : '故障' }}
            </td>
            <td>{{ pile.totalCharges || 0 }}</td>
            <td>{{ pile.totalTime || 0 }}</td>
            <td>{{ pile.totalPower || 0 }}</td>
            <td>
              <div class="action-buttons">
                <button @click="goToDetail(pile.id)" class="detail-btn">查看详情</button>
                <button 
                  @click="confirmDelete(pile.id)" 
                  class="delete-btn"
                  :disabled="pile.isWorking && pile.status === 'CHARGING'"
                  :title="pile.isWorking && pile.status === 'CHARGING' ? '充电中的桩无法删除' : '删除充电桩'"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <!-- 慢充桩区域 -->
      <div class="pile-section">
        <h3 class="section-title">
          <span class="section-icon">🔋</span>
          慢充桩（{{ slowPiles.length }}个）
        </h3>
        <div v-if="slowPiles.length === 0" class="no-piles">
          暂无慢充桩
        </div>
        <table v-else class="pile-table">
          <thead>
          <tr>
            <th>充电桩编号</th>
            <th>是否正常工作</th>
            <th>累计充电次数</th>
            <th>累计充电时长（分钟）</th>
            <th>累计充电电量（kWh）</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="pile in slowPiles" :key="pile.id">
            <td class="pile-number slow-pile">{{ pile.id }}</td>
            <td :class="pile.isWorking ? 'working' : 'not-working'">
              {{ pile.isWorking ? '正常' : '故障' }}
            </td>
            <td>{{ pile.totalCharges || 0 }}</td>
            <td>{{ pile.totalTime || 0 }}</td>
            <td>{{ pile.totalPower || 0 }}</td>
            <td>
              <div class="action-buttons">
                <button @click="goToDetail(pile.id)" class="detail-btn">查看详情</button>
                <button 
                  @click="confirmDelete(pile.id)" 
                  class="delete-btn"
                  :disabled="pile.isWorking && pile.status === 'CHARGING'"
                  :title="pile.isWorking && pile.status === 'CHARGING' ? '充电中的桩无法删除' : '删除充电桩'"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="footer-buttons">
      <button @click="goBackToConsole" class="back-btn">返回控制台</button>
    </div>

    <!-- 添加充电桩对话框 -->
    <div v-if="showAddDialog" class="dialog-overlay" @click="closeAddDialog">
      <div class="dialog-content" @click.stop>
        <h3>添加新充电桩</h3>
        <div class="form-group">
          <label for="pileType">充电桩类型：</label>
          <select id="pileType" v-model="newPileType" class="type-select">
            <option value="">请选择类型</option>
            <option value="FAST">快充桩</option>
            <option value="SLOW">慢充桩</option>
          </select>
        </div>
        <div class="dialog-actions">
          <button @click="closeAddDialog" class="cancel-btn">取消</button>
          <button 
            @click="addPile" 
            :disabled="!newPileType || addLoading"
            class="confirm-btn"
          >
            {{ addLoading ? '添加中...' : '确认添加' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <div v-if="showDeleteDialog" class="dialog-overlay" @click="closeDeleteDialog">
      <div class="dialog-content" @click.stop>
        <h3>确认删除</h3>
        <p>确定要删除充电桩 <strong>{{ deleteTarget }}</strong> 吗？</p>
        <p class="warning-text">⚠️ 此操作不可恢复</p>
        <div class="dialog-actions">
          <button @click="closeDeleteDialog" class="cancel-btn">取消</button>
          <button 
            @click="deletePile" 
            :disabled="deleteLoading"
            class="delete-confirm-btn"
          >
            {{ deleteLoading ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAdmin } from '@/utils/useApi'

const router = useRouter()
const pileStatusList = ref([])

// 对话框状态
const showAddDialog = ref(false)
const showDeleteDialog = ref(false)
const newPileType = ref('')
const deleteTarget = ref('')
const addLoading = ref(false)
const deleteLoading = ref(false)

// 使用统一的API
const { loading, error, getPileStatus, addChargingPile, deleteChargingPile } = useAdmin()

// 计算属性：分离快充和慢充桩
const fastPiles = computed(() => {
  return pileStatusList.value.filter(pile => pile.type === 'fast')
})

const slowPiles = computed(() => {
  return pileStatusList.value.filter(pile => pile.type === 'slow')
})

onMounted(async () => {
  await fetchPileStatus()
})

// 获取充电桩状态
async function fetchPileStatus() {
  try {
    const result = await getPileStatus()
    if (result) {
      pileStatusList.value = result
      console.log('✅ 充电桩列表数据:', pileStatusList.value)
    }
  } catch (e) {
    console.error('❌ 获取充电桩状态失败', e)
  }
}

// 刷新数据
async function refreshData() {
  await fetchPileStatus()
}

// 添加充电桩
async function addPile() {
  if (!newPileType.value) {
    alert('请选择充电桩类型')
    return
  }

  addLoading.value = true
  try {
    const result = await addChargingPile(newPileType.value)
    if (result && result.success) {
      alert(`充电桩添加成功！编号：${result.pileNumber}`)
      closeAddDialog()
      await refreshData() // 刷新列表
    } else {
      alert(result?.message || '添加充电桩失败')
    }
  } catch (e) {
    console.error('❌ 添加充电桩失败', e)
    alert('添加充电桩失败：' + (e.message || '未知错误'))
  } finally {
    addLoading.value = false
  }
}

// 确认删除
function confirmDelete(pileId) {
  deleteTarget.value = pileId
  showDeleteDialog.value = true
}

// 删除充电桩
async function deletePile() {
  if (!deleteTarget.value) return

  deleteLoading.value = true
  try {
    const result = await deleteChargingPile(deleteTarget.value)
    if (result && result.success) {
      alert('充电桩删除成功！')
      closeDeleteDialog()
      await refreshData() // 刷新列表
    } else {
      alert(result?.message || '删除充电桩失败')
    }
  } catch (e) {
    console.error('❌ 删除充电桩失败', e)
    alert('删除充电桩失败：' + (e.message || '未知错误'))
  } finally {
    deleteLoading.value = false
  }
}

// 关闭添加对话框
function closeAddDialog() {
  showAddDialog.value = false
  newPileType.value = ''
}

// 关闭删除对话框
function closeDeleteDialog() {
  showDeleteDialog.value = false
  deleteTarget.value = ''
}

function goToDetail(pileId) {
  router.push(`/admin/pile/${pileId}`)
}

function goBackToConsole() {
  router.push('/admin/console')
}
</script>

<style scoped>
.pile-list {
  padding: 2rem;
  font-family: Arial, sans-serif;
  max-width: 1600px;
  margin: 0 auto;
}

h2 {
  text-align: center;
  margin-bottom: 2rem;
  color: #333;
  font-size: 1.6rem;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.toolbar-left {
  display: flex;
  gap: 1rem;
}

.toolbar-right {
  display: flex;
  gap: 1rem;
}

.add-btn {
  padding: 0.6rem 1.2rem;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.add-btn:hover {
  background-color: #218838;
  transform: translateY(-1px);
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

.pile-sections {
  display: flex;
  gap: 3rem;
}

.pile-section {
  flex: 1;
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid #e0e0e0;
}

.pile-section:first-child {
  border-left: 4px solid #ff6b35; /* 快充桩区域橙色边框 */
}

.pile-section:last-child {
  border-left: 4px solid #4caf50; /* 慢充桩区域绿色边框 */
}

.section-title {
  margin-bottom: 2rem;
  color: #333;
  font-weight: 600;
  font-size: 1.3rem;
}

.section-icon {
  margin-right: 0.5rem;
}

.no-piles {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.pile-table {
  width: 100%;
  border-collapse: collapse;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  border-radius: 8px;
  overflow: hidden;
}

.pile-table th,
.pile-table td {
  border: 1px solid #e0e0e0;
  padding: 1rem 0.75rem;
  text-align: center;
}

.pile-table th {
  background-color: #f8f9fa;
  font-weight: 600;
  color: #333;
  font-size: 0.95rem;
}

.pile-table tbody tr {
  height: 60px;
}

.pile-table tbody tr:nth-child(even) {
  background-color: #fafafa;
}

.pile-table tbody tr:hover {
  background-color: #e3f2fd;
  transition: background-color 0.2s ease;
}

.working {
  color: #52c41a;
  font-weight: bold;
}

.not-working {
  color: #ff4d4f;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  gap: 0.8rem;
  justify-content: center;
  flex-wrap: wrap;
}

.detail-btn {
  padding: 0.6rem 1rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.detail-btn:hover {
  background-color: #0056b3;
  transform: translateY(-1px);
}

.delete-btn {
  padding: 0.6rem 1rem;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.delete-btn:hover:not(:disabled) {
  background-color: #c82333;
  transform: translateY(-1px);
}

.delete-btn:disabled {
  background-color: #ffcccb;
  color: #999;
  cursor: not-allowed;
  transform: none;
}

.footer-buttons {
  display: flex;
  justify-content: center;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 3rem;
  padding-top: 2rem;
}

.refresh-btn {
  padding: 0.6rem 1.4rem;
  background-color: #52c41a;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.refresh-btn:hover:not(:disabled) {
  background-color: #389e0d;
  transform: translateY(-1px);
}

.refresh-btn:disabled {
  background-color: #d9f7be;
  color: #999;
  cursor: not-allowed;
  transform: none;
}

.back-btn {
  padding: 0.6rem 1.4rem;
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

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog-content {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.3);
  min-width: 300px;
  max-width: 500px;
}

.dialog-content h3 {
  margin: 0 0 1.5rem 0;
  color: #333;
  text-align: center;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #495057;
}

.type-select {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: border-color 0.2s ease;
}

.type-select:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.warning-text {
  color: #ff6b35;
  font-size: 0.9rem;
  margin: 0.5rem 0;
  text-align: center;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.5rem;
}

.cancel-btn {
  padding: 0.6rem 1.2rem;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  background-color: #545b62;
}

.confirm-btn {
  padding: 0.6rem 1.2rem;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.confirm-btn:hover:not(:disabled) {
  background-color: #218838;
}

.confirm-btn:disabled {
  background-color: #d4edda;
  color: #999;
  cursor: not-allowed;
}

.delete-confirm-btn {
  padding: 0.6rem 1.2rem;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.delete-confirm-btn:hover:not(:disabled) {
  background-color: #c82333;
}

.delete-confirm-btn:disabled {
  background-color: #ffcccb;
  color: #999;
  cursor: not-allowed;
}

.pile-number {
  font-weight: 600;
}

.fast-pile {
  color: #ff6b35;
}

.slow-pile {
  color: #4caf50;
}

@media (max-width: 768px) {
  .pile-list {
    padding: 1rem;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .toolbar-left,
  .toolbar-right {
    justify-content: center;
  }
  
  .pile-sections {
    flex-direction: column;
  }
  
  .pile-section {
    margin-bottom: 1.5rem;
  }
  
  .pile-table {
    font-size: 0.85rem;
  }
  
  .pile-table th,
  .pile-table td {
    padding: 0.5rem 0.25rem;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 0.25rem;
  }
  
  .footer-buttons {
    flex-direction: column;
  }
  
  .refresh-btn,
  .back-btn {
    width: 100%;
  }
  
  .dialog-content {
    margin: 1rem;
    min-width: auto;
    max-width: none;
  }
}
</style>
