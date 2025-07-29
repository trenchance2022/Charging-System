<template>
  <div class="modify-request">
    <h2>修改充电请求</h2>

    <!-- 电池信息显示 -->
    <div v-if="batteryInfo" class="battery-info">
      <p>电池容量：{{ batteryInfo.capacity }} kWh</p>
      <p>当前电量：{{ batteryInfo.currentPower }} kWh</p>
      <p>最大可充电量：{{ maxAllowedAmount }} kWh</p>
    </div>

    <!-- 电池容量未设置提示 -->
    <div v-if="message && messageType === 'error' && message.includes('请先设置电池容量')" class="battery-setup-prompt">
      <p>{{ message }}</p>
      <button type="button" @click="goToConsole" class="setup-button">前往控制台设置电池容量</button>
    </div>

    <form @submit.prevent="submit">
      <label>充电模式：</label>
      <div class="mode-group">
        <label>
          <input type="radio" value="fast" v-model="chargingMode" :disabled="isModifyInChargingArea" />
          快充
        </label>
        <label>
          <input type="radio" value="slow" v-model="chargingMode" :disabled="isModifyInChargingArea" />
          慢充
        </label>
      </div>

      <label>请求充电量 (kWh)：</label>
      <input
          type="number"
          v-model.number="chargingAmount"
          :min="0.1"
          :max="maxAllowedAmount"
          step="0.1"
          required
          placeholder="请输入充电量"
          :disabled="isModifyInChargingArea"
      />
      <span v-if="chargingAmountError && !isModifyInChargingArea" class="validation-error">{{ chargingAmountError }}</span>

      <div v-if="isModifyInChargingArea" class="warning">
        注意：在充电区无法修改充电请求。如需修改，请先取消充电请求后重新提交。
      </div>

      <div class="button-group">
        <button type="submit" :disabled="isModifyInChargingArea || !isValidRequest">保存修改</button>
        <button type="button" @click="goBack" class="secondary">返回</button>
      </div>

      <p v-if="message && !(message.includes('请先设置电池容量'))" :class="messageType">{{ message }}</p>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import apiService from '../utils/api';

const router = useRouter();

const chargingMode = ref('fast');
const chargingAmount = ref(10);
const message = ref('');
const messageType = ref('success');
const requestStatus = ref('');
const requestLocation = ref(''); // waiting_area 或 charging_area
const batteryInfo = ref(null);

// 是否在充电区修改（不允许修改充电模式和充电量）
const isModifyInChargingArea = computed(() => {
  return requestStatus.value === 'CHARGING' || 
    (requestStatus.value === 'WAITING' && requestLocation.value === 'charging_area');
});

// 计算最大可充电量
const maxAllowedAmount = computed(() => {
  if (!batteryInfo.value) return 0;
  return Math.max(0, batteryInfo.value.capacity - batteryInfo.value.currentPower);
});

// 充电量验证错误信息
const chargingAmountError = computed(() => {
  if (!chargingAmount.value || !batteryInfo.value) return '';
  
  if (chargingAmount.value <= 0) {
    return '请求充电量必须大于0';
  }
  
  if (chargingAmount.value > maxAllowedAmount.value) {
    return `请求充电量不能超过最大可充电量 ${maxAllowedAmount.value.toFixed(1)} kWh`;
  }
  
  return '';
});

// 验证请求是否有效
const isValidRequest = computed(() => {
  return batteryInfo.value && 
         chargingAmount.value > 0 && 
         chargingAmount.value <= maxAllowedAmount.value &&
         !chargingAmountError.value;
});

// 获取电池信息
async function fetchBatteryInfo() {
  try {
    console.log('ModifyRequest: 开始获取电池信息');
    
    // 获取电池容量
    console.log('ModifyRequest: 正在获取电池容量...');
    const batteryCapacityRes = await apiService.charge.getBatteryCapacity();
    console.log('ModifyRequest: 电池容量获取成功', batteryCapacityRes);
    
    // 检查是否设置了电池容量
    if (!batteryCapacityRes.batteryCapacity || batteryCapacityRes.batteryCapacity <= 0) {
      message.value = '请先设置电池容量后再修改充电请求';
      messageType.value = 'error';
      return;
    }
    
    // 获取充电状态（包含当前电量）
    console.log('ModifyRequest: 正在获取用户状态...');
    const statusRes = await apiService.charge.getUserStatus();
    console.log('ModifyRequest: 用户状态获取成功', statusRes);
    
    batteryInfo.value = {
      capacity: batteryCapacityRes.batteryCapacity || 0,
      currentPower: statusRes.currentPower || 0
    };
    
    console.log('ModifyRequest: 电池信息设置完成', batteryInfo.value);
  } catch (error) {
    console.error('ModifyRequest: 获取电池信息失败:', error);
    console.error('ModifyRequest: 错误详情:', {
      message: error.message,
      response: error.response,
      status: error.response?.status,
      data: error.response?.data
    });
    message.value = `获取电池信息失败：${error.message}`;
    messageType.value = 'error';
  }
}

// 获取当前请求的信息
async function fetchRequestInfo() {
  try {
    const res = await apiService.charge.getUserStatus();
    requestStatus.value = res.status;
    
    // 如果没有活跃请求，跳转回控制台
    if (res.status === 'NOT_FOUND') {
      message.value = '没有找到活跃的充电请求';
      messageType.value = 'error';
      setTimeout(() => router.push('/user/console'), 1500);
      return;
    }
    
    // 设置充电模式（转换为与前端匹配的值）
    if (res.chargingMode) {
      chargingMode.value = res.chargingMode;
    }
    
    // 设置充电量 - 使用请求的充电量而不是电池总容量
    if (res.requestedAmount) {
      chargingAmount.value = res.requestedAmount;
    }
    
    // 判断是在等候区还是充电区
    if (res.chargingPileId) {
      requestLocation.value = 'charging_area';
    } else {
      requestLocation.value = 'waiting_area';
    }
  } catch (err) {
    message.value = '获取充电请求信息失败';
    messageType.value = 'error';
    console.error('获取充电请求信息失败:', err);
  }
}

onMounted(async () => {
  await fetchBatteryInfo();
  await fetchRequestInfo();
});

async function submit() {
  try {
    if (!isValidRequest.value) {
      message.value = chargingAmountError.value || '请检查输入的充电量';
      messageType.value = 'error';
      return;
    }

    const token = localStorage.getItem('jwt');
    if (!token) {
      message.value = '非法登录';
      messageType.value = 'error';
      return;
    }

    const requestData = {
      chargingMode: chargingMode.value,
      chargingAmount: chargingAmount.value
    };
    
    const res = await apiService.charge.modifyRequest(requestData);

    message.value = `修改成功：${res.message}`;
    messageType.value = 'success';
    setTimeout(() => router.push('/user/console'), 1500);
  } catch (err) {
    message.value = err.message || '修改失败';
    messageType.value = 'error';
    console.error('修改失败:', err);
  }
}

function goBack() {
  router.push('/user/console');
}

function goToConsole() {
  router.push('/user/console');
}
</script>

<style scoped>
.modify-request {
  max-width: 480px;
  margin: 2rem auto;
  padding: 1.5rem;
  background: #f9fafc;
  border-radius: 8px;
  box-shadow: 0 4px 8px rgb(0 0 0 / 0.1);
}

h2 {
  color: #409eff;
  margin-bottom: 1rem;
}

label {
  display: block;
  margin: 1rem 0 0.5rem;
  font-weight: 500;
}

input[type='number'] {
  width: 100%;
  padding: 0.5rem;
  font-size: 1rem;
  box-sizing: border-box;
}

.mode-group {
  display: flex;
  gap: 2rem;
  margin-top: 0.5rem;
}

.mode-group input {
  margin-right: 0.4rem;
}

.button-group {
  margin-top: 1.5rem;
  display: flex;
  justify-content: space-between;
}

button {
  padding: 0.6rem 1.2rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  background-color: #409eff;
  color: white;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

button:hover:not(:disabled) {
  background-color: #66b1ff;
}

button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

button.secondary {
  background-color: #e6e6e6;
  color: #333;
}

.success {
  color: green;
  margin-top: 1rem;
}

.error {
  color: red;
  margin-top: 1rem;
}

.warning {
  margin-top: 1rem;
  padding: 0.8rem;
  background-color: #fff6e6;
  border-left: 4px solid #f90;
  color: #666;
  border-radius: 4px;
  font-size: 0.9rem;
}

/* 禁用状态的样式 */
input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
  opacity: 0.7;
}

.back-btn {
  background: none;
  border: none;
  color: #409eff;
  cursor: pointer;
  font-size: 1rem;
  margin-bottom: 1rem;
  padding: 0;
}
.back-btn:hover {
  text-decoration: underline;
}

.battery-info {
  margin-bottom: 1rem;
  padding: 0.8rem;
  background-color: #fff6e6;
  border-left: 4px solid #f90;
  color: #666;
  border-radius: 4px;
  font-size: 0.9rem;
}

.validation-error {
  color: red;
  margin-top: 0.5rem;
  font-size: 0.8rem;
}

.battery-setup-prompt {
  margin-bottom: 1rem;
  padding: 0.8rem;
  background-color: #fff6e6;
  border-left: 4px solid #f90;
  color: #666;
  border-radius: 4px;
  font-size: 0.9rem;
}

.setup-button {
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0.6rem 1.2rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.setup-button:hover {
  background-color: #66b1ff;
}
</style>
