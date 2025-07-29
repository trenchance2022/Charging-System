<!-- src/views/AdminViews/SystemSettings.vue -->
<template>
  <div class="settings-page">
    <h2>系统参数设置</h2>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error-message">{{ error }}</div>

    <form v-if="!loading && !error && configItems.length > 0" @submit.prevent="saveConfig">
      <div class="config-list">
        <div 
          v-for="item in configItems" 
          :key="item.key" 
          class="config-item"
        >
          <div class="config-header">
            <label :for="item.key" class="config-label">{{ item.displayName }}</label>
            <!-- 普通输入框 -->
            <input 
              v-if="item.type !== 'select'"
              :id="item.key"
              :type="item.type"
              v-model.number="configValues[item.key]"
              :min="item.type === 'number' ? 0 : undefined"
              required 
              class="config-input"
            />
            <!-- 选择框 -->
            <select 
              v-else
              :id="item.key"
              v-model="configValues[item.key]"
              required 
              class="config-input config-select"
            >
              <option v-for="option in getSelectOptions(item.key)" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div v-if="item.description" class="config-description">{{ item.description }}</div>
        </div>
      </div>

      <div class="action-buttons">
        <button 
          type="submit" 
          :disabled="saveLoading"
          class="save-btn"
        >
          {{ saveLoading ? '保存中...' : '保存设置' }}
        </button>
        <button type="button" @click="goBack" class="back-btn">返回控制台</button>
      </div>
    </form>

    <div v-if="!loading && !error && configItems.length === 0" class="no-config">
      暂无可配置的系统参数
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdmin } from '@/utils/useApi'

const router = useRouter()

const configItems = ref([])
const configValues = reactive({})
const saveLoading = ref(false)

// 使用统一的API
const { loading, error, getSystemConfig, updateSystemConfig } = useAdmin()

onMounted(async () => {
  await fetchConfig()
})

// 获取系统配置
async function fetchConfig() {
  try {
    const result = await getSystemConfig()
    if (result && Array.isArray(result)) {
      configItems.value = result
      
      // 初始化配置值
      result.forEach(item => {
        if (item.type === 'number') {
          configValues[item.key] = parseInt(item.value) || 0
        } else {
          configValues[item.key] = item.value || ''
        }
      })
    }
  } catch (e) {
    console.error('❌ 获取系统配置失败', e)
  }
}

// 获取选择框的选项
function getSelectOptions(configKey) {
  switch (configKey) {
    case 'schedule_strategy':
      return [
        { value: 'ORIGINAL', label: '原始算法' },
        { value: 'SINGLE_BATCH_OPTIMAL', label: '单批次最优' },
        { value: 'FULL_BATCH_OPTIMAL', label: '全批次最优' }
      ]
    default:
      return []
  }
}

// 保存配置
async function saveConfig() {
  saveLoading.value = true
  try {
    // 将配置值转换为字符串传给后端
    const configData = {}
    configItems.value.forEach(item => {
      if (item.type === 'number') {
        configData[item.key] = configValues[item.key].toString()
      } else {
        configData[item.key] = configValues[item.key]
      }
    })

    const result = await updateSystemConfig(configData)
    
    if (result && result.success) {
      alert('保存成功')
      // 重新加载配置数据而不是跳转
      await fetchConfig()
    } else {
      alert(result?.message || '保存失败')
    }
  } catch (e) {
    console.error('❌ 保存系统配置失败', e)
    alert('保存失败')
  } finally {
    saveLoading.value = false
  }
}

function goBack() {
  router.push('/admin/console')
}
</script>

<style scoped>
.settings-page {
  max-width: 600px;
  margin: 2rem auto;
  font-family: Arial, sans-serif;
  padding: 0 1rem;
}

h2 {
  text-align: center;
  color: #333;
  margin-bottom: 2rem;
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

.no-config {
  text-align: center;
  padding: 2rem;
  color: #666;
  font-size: 1.1rem;
}

form {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.config-list {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1rem;
  border: 1px solid #e9ecef;
}

.config-item {
  padding: 0.75rem 0;
  border-bottom: 1px solid #e9ecef;
}

.config-item:last-child {
  border-bottom: none;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.config-label {
  font-weight: 600;
  color: #495057;
  font-size: 0.95rem;
  flex: 1;
  min-width: 0;
}

.config-input {
  width: 120px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.9rem;
  transition: all 0.2s ease;
  text-align: center;
  flex-shrink: 0;
}

.config-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.config-select {
  width: 150px;
  text-align: left;
  cursor: pointer;
}

.config-description {
  font-size: 0.8rem;
  color: #6c757d;
  line-height: 1.3;
  margin-top: 0.5rem;
  padding-left: 0;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.save-btn {
  padding: 0.75rem 2rem;
  background-color: #007bff;
  color: white;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.95rem;
}

.save-btn:hover:not(:disabled) {
  background-color: #0056b3;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.3);
}

.save-btn:disabled {
  background-color: #d0e5ff;
  color: #999;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.back-btn {
  padding: 0.75rem 2rem;
  background-color: #6c757d;
  color: white;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 0.95rem;
}

.back-btn:hover {
  background-color: #545b62;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

@media (max-width: 768px) {
  .settings-page {
    margin: 1rem auto;
    padding: 0 0.5rem;
  }
  
  .config-header {
    flex-direction: column;
    align-items: stretch;
    gap: 0.5rem;
  }
  
  .config-input {
    width: 100%;
    text-align: left;
  }
  
  .action-buttons {
    flex-direction: column;
  }
  
  .save-btn,
  .back-btn {
    width: 100%;
  }
}
</style>
