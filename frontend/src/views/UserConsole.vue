<template>
  <div>
    <h2>普通用户控制台</h2>
    
    <!-- 通知显示区域 -->
    <div v-if="store.notifications.length > 0" class="notifications-container">
      <div 
        v-for="notification in store.notifications" 
        :key="notification.id"
        :class="['notification', `notification-${notification.level.toLowerCase()}`]"
      >
        <div class="notification-header">
          <h4>{{ notification.title }}</h4>
          <button @click="store.removeNotification(notification.id)" class="close-btn">&times;</button>
        </div>
        <p class="notification-message">{{ notification.message }}</p>
        <small class="notification-time">{{ formatTime(notification.notificationTime) }}</small>
      </div>
    </div>
    
    <div class="container">
      <ChargeStatus />
      <QueueInfo />
      <StationInfo />
    </div>
    <ChargeControls />
    <div class="logout-container">
      <button @click="logout">退出系统</button>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import ChargeStatus from '../components/ChargeStatus.vue';
import QueueInfo from '../components/QueueInfo.vue';
import StationInfo from '../components/StationInfo.vue';
import ChargeControls from '../components/ChargeControls.vue';
import { useChargeStore } from '../stores/chargeStatus';
import apiService from '../utils/api';

const router = useRouter();
const store = useChargeStore();

let eventSource = null;

// 连接通知SSE
function connectNotifications() {
  // 关闭之前的连接
  if (eventSource) {
    eventSource.close();
  }
  
  const token = localStorage.getItem('jwt');
  if (!token) {
    console.error('未找到JWT令牌，无法建立通知SSE连接');
    return;
  }
  
  const url = `/api/notifications/connect?token=${token}`;
  eventSource = new EventSource(url);
  
  eventSource.onmessage = function(event) {
    try {
      const notification = JSON.parse(event.data);
      
      // 跳过系统连接确认消息
      if (notification.notificationType === 'SYSTEM') {
        console.log('通知连接已建立');
        return;
      }
      
      // 使用store管理通知
      store.addNotification(notification);
      
    } catch (error) {
      console.error('解析通知消息失败:', error);
    }
  };
  
  eventSource.onerror = function(event) {
    console.error('通知连接错误:', event);
    eventSource.close();
    
    // 3秒后尝试重新连接
    setTimeout(connectNotifications, 3000);
  };
}

// 格式化时间显示
function formatTime(timeString) {
  if (!timeString) return '';
  const date = new Date(timeString);
  return date.toLocaleString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function logout() {
  // 断开通知连接
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
  
  // 清理登录状态
  localStorage.removeItem('jwt');
  router.push('/login');
}

onMounted(() => {
  // 页面加载时连接通知
  connectNotifications();
});

onUnmounted(() => {
  // 页面卸载时断开连接
  if (eventSource) {
    eventSource.close();
    eventSource = null;
  }
});
</script>

<style scoped>
.container {
  display: flex;
  gap: 2rem;
  margin-bottom: 1.5rem;
  justify-content: center;  /* 居中对齐 */
}
h2 {
  text-align: center;
  margin-bottom: 1.5rem; /* 适当的下边距 */
}
.logout-container {
  display: flex;
  justify-content: center;
  margin-top: 2rem;
}

.logout-container button {
  background-color: #f56c6c;
  color: white;
  border: none;
  padding: 0.6rem 1.2rem;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.logout-container button:hover {
  background-color: #ff8787;
}

/* 通知样式 */
.notifications-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1000;
  max-width: 400px;
}

.notification {
  background: white;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 0.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-left: 4px solid #409eff;
  animation: slideIn 0.3s ease-out;
}

.notification-info {
  border-left-color: #409eff;
}

.notification-warning {
  border-left-color: #e6a23c;
}

.notification-error {
  border-left-color: #f56c6c;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.notification-header h4 {
  margin: 0;
  color: #303133;
  font-size: 1rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #909399;
  padding: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  color: #f56c6c;
}

.notification-message {
  margin: 0 0 0.5rem 0;
  color: #606266;
  line-height: 1.4;
}

.notification-time {
  color: #909399;
  font-size: 0.85rem;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
