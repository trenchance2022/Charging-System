<template>
  <section class="register-charge">
    <h2>提交充电请求</h2>
    
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
    
    <form @submit.prevent="submitRequest">
      <label>
        充电模式：
        <select v-model="chargingMode">
          <option value="fast">快充</option>
          <option value="slow">慢充</option>
        </select>
      </label>
      <label>
        请求充电量（kWh）：
        <input 
          type="number" 
          v-model.number="chargingAmount" 
          :min="0.1" 
          :max="maxAllowedAmount"
          step="0.01"
        />
        <span v-if="chargingAmountError" class="validation-error">{{ chargingAmountError }}</span>
      </label>
      <button type="submit" :disabled="!isValidRequest">提交</button>
      <button type="button" @click="goBack">返回</button>
      <p v-if="message && !(message.includes('请先设置电池容量'))" :class="messageType">{{ message }}</p>
    </form>
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiService from '../utils/api';

const router = useRouter();
const chargingMode = ref('fast');
const chargingAmount = ref(10);
const message = ref('');
const messageType = ref('success');
const batteryInfo = ref(null);

// 计算最大可充电量
const maxAllowedAmount = computed(() => {
  if (!batteryInfo.value) return 0;
  const result = batteryInfo.value.capacity - batteryInfo.value.currentPower;
  return Math.max(0, parseFloat(result.toFixed(2)));
});

// 充电量验证错误信息
const chargingAmountError = computed(() => {
  if (!chargingAmount.value || !batteryInfo.value) return '';
  
  if (chargingAmount.value <= 0) {
    return '请求充电量必须大于0';
  }
  
  if (chargingAmount.value > maxAllowedAmount.value) {
    return `请求充电量不能超过最大可充电量 ${maxAllowedAmount.value.toFixed(2)} kWh`;
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
    console.log('RegisterChargeRequest: 开始获取电池信息');
    
    // 获取电池容量
    console.log('RegisterChargeRequest: 正在获取电池容量...');
    const batteryCapacityRes = await apiService.charge.getBatteryCapacity();
    console.log('RegisterChargeRequest: 电池容量获取成功', batteryCapacityRes);
    
    // 检查是否设置了电池容量
    if (!batteryCapacityRes.batteryCapacity || batteryCapacityRes.batteryCapacity <= 0) {
      message.value = '请先设置电池容量后再提交充电请求';
      messageType.value = 'error';
      return;
    }
    
    // 获取充电状态（包含当前电量）
    console.log('RegisterChargeRequest: 正在获取用户状态...');
    const statusRes = await apiService.charge.getUserStatus();
    console.log('RegisterChargeRequest: 用户状态获取成功', statusRes);
    
    batteryInfo.value = {
      capacity: batteryCapacityRes.batteryCapacity || 0,
      currentPower: statusRes.currentPower || 0
    };
    
    console.log('RegisterChargeRequest: 电池信息设置完成', batteryInfo.value);
    
    // 设置默认充电量为最大可充电量的一半
    if (maxAllowedAmount.value > 0) {
      chargingAmount.value = Math.round(maxAllowedAmount.value / 2 * 100) / 100;
      console.log('RegisterChargeRequest: 默认充电量设置为', chargingAmount.value);
    } else {
      chargingAmount.value = 1; // 如果没有可充电量，设置为1作为默认值
    }
  } catch (error) {
    console.error('RegisterChargeRequest: 获取电池信息失败:', error);
    console.error('RegisterChargeRequest: 错误详情:', {
      message: error.message,
      response: error.response,
      status: error.response?.status,
      data: error.response?.data
    });
    message.value = `获取电池信息失败：${error.message}`;
    messageType.value = 'error';
  }
}

onMounted(() => {
  fetchBatteryInfo();
});

async function submitRequest() {
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
    
    const res = await apiService.charge.submitRequest(requestData);

    message.value = `提交成功：${res.message}`;
    messageType.value = 'success';
    setTimeout(() => router.push('/user/console'), 1500);
  } catch (err) {
    message.value = err.message || '提交失败';
    messageType.value = 'error';
    console.error('提交失败:', err);
  }
}

function goBack() {
  router.back();
}

function goToConsole() {
  router.push('/user/console');
}
</script>

<style scoped>
.register-charge {
  max-width: 400px;
  margin: 2rem auto;
  background: #fff;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.battery-info {
  background: #f0f8ff;
  padding: 1rem;
  border-radius: 6px;
  margin-bottom: 1.5rem;
  border: 1px solid #e1f2ff;
}

.battery-info p {
  margin: 0.3rem 0;
  color: #333;
  font-size: 0.9rem;
}

label {
  display: block;
  margin-bottom: 1rem;
}

input,
select {
  width: 100%;
  padding: 0.5rem;
  margin-top: 0.3rem;
}

.validation-error {
  display: block;
  color: red;
  font-size: 0.8rem;
  margin-top: 0.3rem;
}

button {
  margin-right: 0.8rem;
  padding: 0.6rem 1rem;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover:not(:disabled) {
  background-color: #66b1ff;
}

button:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
  opacity: 0.7;
}

.success {
  margin-top: 1rem;
  color: green;
}

.error {
  margin-top: 1rem;
  color: red;
}

.battery-setup-prompt {
  background: #fff3f3;
  padding: 1rem;
  border-radius: 6px;
  margin-bottom: 1.5rem;
  border: 1px solid #ffd6d6;
}

.battery-setup-prompt p {
  margin: 0.3rem 0;
  color: #333;
  font-size: 0.9rem;
}

.setup-button {
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 0.6rem 1rem;
  cursor: pointer;
}

.setup-button:hover {
  background-color: #66b1ff;
}
</style>
