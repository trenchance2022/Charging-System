<template>
  <section class="charge-controls">
    <h3>充电操作</h3>
    <div class="button-group">
      <button 
        @click="submitCharge" 
        :disabled="store.isInChargingArea || !store.canSubmitRequest" 
        :class="{ disabled: store.isInChargingArea || !store.canSubmitRequest }"
        :title="getSubmitButtonTitle()"
      >
        {{ store.hasActiveRequest ? '修改充电请求' : '提交充电请求' }}
      </button>
      <button 
        @click="startCharge" 
        :disabled="!store.canStartCharge"
        :class="{ disabled: !store.canStartCharge }"
        :title="getStartChargeButtonTitle()"
      >开始充电</button>
      <button 
        @click="stopCharge" 
        :disabled="!store.canStopCharge"
        :class="{ disabled: !store.canStopCharge }"
        :title="!store.canStopCharge ? '只有在充电中状态才能结束充电' : ''"
      >结束充电</button>
      <button 
        @click="cancelCharge" 
        :disabled="!store.canCancelCharge"
        :class="{ disabled: !store.canCancelCharge }"
        :title="!store.canCancelCharge ? '只有有请求在排队时才能取消充电' : ''"
      >取消充电</button>
      <button 
        @click="showBatteryCapacityDialog" 
        :disabled="!store.canSetBatteryCapacity"
        :class="{ disabled: !store.canSetBatteryCapacity }"
        :title="!store.canSetBatteryCapacity ? '有活跃充电请求时无法修改电池容量' : ''"
      >设置电池容量</button>
      <button @click="goToBillList">查看充电详单信息</button>
    </div>
    
    <!-- 电池容量设置对话框 -->
    <div v-if="showDialog" class="dialog-overlay" @click="closeDialog">
      <div class="dialog" @click.stop>
        <h4>设置电池容量</h4>
        <div class="dialog-content">
          <div class="current-info">
            <div class="current-capacity" v-if="store.batteryCapacity">
              <p>当前电池容量：{{ store.batteryCapacity }} kWh</p>
            </div>
            <div class="current-power">
              <p>当前电量：{{ (store.currentPower || 0).toFixed(1) }} kWh</p>
            </div>
          </div>
          <div class="input-group">
            <label for="batteryCapacity">新电池容量：</label>
            <input 
              id="batteryCapacity"
              type="number" 
              v-model="newBatteryCapacity" 
              :min="store.currentPower || 0.1" 
              step="0.1"
              placeholder="请输入电池容量 (kWh)"
            />
            <span class="unit">kWh</span>
          </div>
          <!-- 显示验证错误信息 -->
          <div v-if="newBatteryCapacity && !isValidCapacity" class="validation-error">
            {{ capacityValidationMessage }}
          </div>
        </div>
        <div class="dialog-actions">
          <button @click="confirmSetBatteryCapacity" :disabled="!isValidCapacity">确定</button>
          <button @click="closeDialog">取消</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from 'vue-router';
import apiService from '../utils/api';
import { onMounted, ref, computed } from 'vue';
import { useChargeStore } from '../stores/chargeStatus'

const router = useRouter();
const store = useChargeStore()

// 对话框相关状态
const showDialog = ref(false)
const newBatteryCapacity = ref('')

// 计算属性
const isValidCapacity = computed(() => {
  const capacity = parseFloat(newBatteryCapacity.value)
  if (isNaN(capacity) || capacity <= 0) {
    return false
  }
  
  // 电池容量不能小于当前电量
  const currentPower = store.currentPower || 0
  if (capacity < currentPower) {
    return false
  }
  
  return true
})

// 获取容量验证的错误信息
const capacityValidationMessage = computed(() => {
  const capacity = parseFloat(newBatteryCapacity.value)
  if (isNaN(capacity) || capacity <= 0) {
    return '请输入有效的电池容量'
  }
  
  const currentPower = store.currentPower || 0
  if (capacity < currentPower) {
    return `电池容量不能小于当前电量 ${currentPower.toFixed(1)} kWh`
  }
  
  return ''
})

// 检查用户是否有活跃充电请求
onMounted(async () => {
  console.log('ChargeControls: 组件挂载，初始化状态检查');
  try {
    await store.refreshAllStatus();
    console.log('ChargeControls: 初始化完成', {
      batteryCapacity: store.batteryCapacity,
      canSubmitRequest: store.canSubmitRequest,
      canSetBatteryCapacity: store.canSetBatteryCapacity
    });
  } catch (error) {
    console.error('ChargeControls: 初始化失败:', error);
  }
});

// 获取提交按钮的提示文本
function getSubmitButtonTitle() {
  if (store.isInChargingArea) {
    return '充电中或在充电区队列中时不能修改请求'
  }
  if (!store.canSubmitRequest) {
    return '请先设置电池容量后再提交充电请求'
  }
  return ''
}

function getStartChargeButtonTitle() {
  if (store.chargeStatus !== 'WAITING') {
    return '只有等待状态的请求才能开始充电'
  }
  if (!store.isQueueFirst) {
    return '只有当请求在充电桩队列队首时才能开始充电'
  }
  if (store.chargingPileStatus === 'UNAVAILABLE') {
    return '充电桩当前不可用，无法开始充电'
  }
  if (store.chargingPileStatus === 'CHARGING') {
    return '充电中正在充电中，无法开始充电'
  }
  if (store.chargingPileStatus !== 'AVAILABLE') {
    return '充电桩状态异常，无法开始充电'
  }
  return ''
}

function submitCharge() {
  // 检查是否可以提交
  if (store.isInChargingArea || !store.canSubmitRequest) {
    if (!store.canSubmitRequest) {
      alert('请先设置电池容量后再提交充电请求');
    }
    return;
  }
  
  console.log('ChargeControls: 跳转到充电请求页面');
  // 根据是否有活跃请求导航到不同页面
  if (store.hasActiveRequest) {
    // 有活跃请求时，导航到修改页面
    router.push('/user/ModifyRequest');
  } else {
    // 没有活跃请求时，导航到提交新请求页面
    router.push('/user/ChargeRequest');
  }
}

async function startCharge() {
  if (!store.canStartCharge) {
    alert(getStartChargeButtonTitle());
    return;
  }
  
  try {
    console.log('ChargeControls: 开始充电操作');
    const response = await apiService.charge.start();
    alert(`开始充电成功：${response.message}`);
    
    // 立即更新状态
    store.startCharging();
    
    // 刷新所有状态确保同步
    await store.refreshAllStatus();
    console.log('ChargeControls: 开始充电操作完成，状态已更新');
  } catch (err) {
    console.error('ChargeControls: 开始充电失败:', err);
    alert(`开始充电失败：${err.message}`);
    // 发生错误时也要刷新状态
    await store.refreshAllStatus();
  }
}

async function stopCharge() {
  if (!store.canStopCharge) {
    alert('只有在充电中状态才能结束充电');
    return;
  }
  
  try {
    console.log('ChargeControls: 结束充电操作');
    const response = await apiService.charge.stop();
    alert(`结束充电成功：${response.message}`);
    
    // 立即更新状态
    store.stopCharging();
    
    // 刷新所有状态确保同步
    await store.refreshAllStatus();
    console.log('ChargeControls: 结束充电操作完成，状态已更新');
  } catch (err) {
    console.error('ChargeControls: 结束充电失败:', err);
    alert(`结束充电失败：${err.message}`);
    // 发生错误时也要刷新状态
    await store.refreshAllStatus();
  }
}

async function cancelCharge() {
  if (!store.canCancelCharge) {
    alert('只有有请求在排队时才能取消充电');
    return;
  }
  
  try {
    console.log('ChargeControls: 取消充电操作');
    const response = await apiService.charge.cancel();
    alert(`取消充电成功：${response.message}`);
    
    // 立即更新状态
    store.setCanceled();
    
    // 刷新所有状态确保同步
    await store.refreshAllStatus();
    console.log('ChargeControls: 取消充电操作完成，状态已更新');
  } catch (err) {
    console.error('ChargeControls: 取消充电失败:', err);
    alert(`取消充电失败：${err.message}`);
    // 发生错误时也要刷新状态
    await store.refreshAllStatus();
  }
}

// 显示电池容量设置对话框
function showBatteryCapacityDialog() {
  if (!store.canSetBatteryCapacity) {
    alert('有活跃充电请求时无法修改电池容量');
    return;
  }
  
  // 设置当前值为初始值
  newBatteryCapacity.value = store.batteryCapacity ? store.batteryCapacity.toString() : ''
  showDialog.value = true
}

// 关闭对话框
function closeDialog() {
  showDialog.value = false
  newBatteryCapacity.value = ''
}

// 确认设置电池容量
async function confirmSetBatteryCapacity() {
  if (!isValidCapacity.value) {
    alert(capacityValidationMessage.value);
    return;
  }
  
  try {
    const capacity = parseFloat(newBatteryCapacity.value)
    const response = await store.setBatteryCapacity(capacity)
    
    if (response.success) {
      alert('电池容量设置成功');
      closeDialog()
      
      // 刷新充电状态以更新电池总量显示
      try {
        await store.fetchChargeStatus()
      } catch (error) {
        console.error('刷新充电状态失败:', error)
      }
    } else {
      alert(`设置失败：${response.message || '未知错误'}`)
    }
  } catch (error) {
    console.error('设置电池容量失败:', error)
    alert(`设置失败：${error.message}`)
  }
}

function goToBillList() {
  router.push('/user/bill/list')
}
</script>

<style scoped>
.charge-controls {
  background: #f9fafc;
  padding: 1rem 1.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgb(0 0 0 / 0.1);
  max-width: 960px;
  margin: 0 auto 2rem;
  box-sizing: border-box;
  text-align: center; /* 文字居中 */
}

h3 {
  margin-bottom: 0.75rem;
  font-weight: 600;
  color: #40bcff;
  border-bottom: 2px solid #409eff;
  padding-bottom: 0.3rem;
}

.button-group {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center; /* 按钮居中 */
}

button {
  background-color: #409eff;
  border: none;
  color: white;
  padding: 0.6rem 1.2rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

button:hover:not(:disabled) {
  background-color: #66b1ff;
}

button:disabled, button.disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
  opacity: 0.7;
}

/* 对话框样式 */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.dialog {
  background: white;
  border-radius: 8px;
  padding: 1.5rem;
  min-width: 400px;
  max-width: 500px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.dialog h4 {
  margin: 0 0 1rem 0;
  color: #409eff;
  font-size: 1.2rem;
  text-align: center;
}

.dialog-content {
  margin-bottom: 1.5rem;
}

.current-info {
  background: #f0f8ff;
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  border: 1px solid #e1f2ff;
}

.current-info p {
  margin: 0;
  color: #666;
  font-size: 0.9rem;
}

.input-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.input-group label {
  min-width: 100px;
  color: #333;
  font-weight: 500;
}

.input-group input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.input-group input:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.unit {
  color: #666;
  font-size: 0.9rem;
  min-width: 30px;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 1rem;
}

.dialog-actions button {
  min-width: 80px;
}

.dialog-actions button:first-child {
  background-color: #409eff;
}

.dialog-actions button:last-child {
  background-color: #666;
}

.dialog-actions button:last-child:hover:not(:disabled) {
  background-color: #777;
}

.validation-error {
  color: red;
  font-size: 0.8rem;
  margin-top: 0.5rem;
}
</style>
