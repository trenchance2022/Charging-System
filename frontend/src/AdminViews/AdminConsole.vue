<template>
  <div class="admin-console">
    <header class="header">管理员控制台</header>

    <div class="main-layout">
      <!-- 左侧功能按钮区 -->
      <aside class="sidebar">
        <div class="nav-title">功能菜单</div>
        <div class="button-group">
          <button @click="goToPileList">查看所有充电桩当前状态</button>
          <button @click="goToReport">报表展示</button>
          <button @click="goToSettings">系统参数设置</button>
          <button @click="fetchPileStatus" :disabled="loading">
            {{ loading ? '刷新中...' : '刷新充电桩状态' }}
          </button>
          <button @click="logout" class="danger">退出系统</button>
        </div>
      </aside>

      <!-- 右侧状态总览区 -->
      <section class="overview">
        <h2>充电桩状态总览</h2>
        
        <!-- 错误信息显示 -->
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <div class="pile-icons">
          <div
              v-for="pile in piles"
              :key="pile.id"
              class="pile-wrapper"
          >
            <!-- 主框 -->
            <div
                class="pile-box"
                :class="[pile.type, activePile === pile.id ? 'active' : '', pile.isWorking ? 'on' : 'off']"
                @click="handleClick(pile.id)"
            >
              <div class="icon">
                <span v-if="pile.type === 'fast'">⚡</span>
                <span v-else>🔋</span>
              </div>
              <div class="label">
                {{ pile.id }}（{{ pile.type === 'fast' ? '快充' : '慢充' }}）<br>
                状态：{{ pile.isWorking ? '开启' : '关闭' }}<br>
                功率：{{ pile.power }}kW
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdmin } from '@/utils/useApi';

const router = useRouter();
const piles = ref([]);
const activePile = ref(null);

// 使用统一的API
const { loading, error, data, getPileStatus } = useAdmin();

function logout() {
  localStorage.removeItem('jwt');
  router.push('/login');
}

function goToPileList() {
  router.push('/admin/ChargingPileList');
}

function goToSettings() {
  router.push('/admin/settings');
}

function goToReport() {
  router.push('/admin/report');
}

function handleClick(pileId) {
  activePile.value = pileId;
  setTimeout(() => {
    router.push(`/admin/pile/${pileId}`);
  }, 200);
}

async function fetchPileStatus() {
  try {
    const result = await getPileStatus();
    if (result) {
      piles.value = result;
      console.log('✅ piles data:', JSON.stringify(piles.value, null, 2));
    }
  } catch (e) {
    console.error('❌ 获取充电桩状态失败', e);
  }
}

onMounted(fetchPileStatus);

</script>

<style scoped>
.admin-console {
  font-family: sans-serif;
}
.header {
  background: #333;
  color: white;
  padding: 1rem;
  font-size: 1.2rem;
}
.main-layout {
  display: flex;
  min-height: 100vh;
}
.sidebar {
  width: 220px;
  padding: 1rem;
  border-right: 2px solid #ccc;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
}
.nav-title {
  font-weight: bold;
  margin-bottom: 1rem;
}
.button-group {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.button-group button {
  padding: 0.6rem 1rem;
  font-size: 0.95rem;
  font-weight: 500;
  border: 2px solid #007bff;
  border-radius: 6px;
  background-color: white;
  color: #007bff;
  cursor: pointer;
  transition: all 0.25s ease;
}
.button-group button:hover:not(:disabled) {
  background-color: #007bff;
  color: white;
}
.button-group button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.button-group .danger {
  border-color: red;
  color: red;
}
.button-group .danger:hover {
  background-color: red;
  color: white;
}
.overview {
  flex: 1;
  padding: 3rem;
}
.overview h2 {
  text-align: center;
  font-size: 1.4rem;
  margin-bottom: 2.5rem;
}
.error-message {
  background-color: #ffebee;
  color: #c62828;
  padding: 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  text-align: center;
}
.pile-icons {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  justify-content: center;
  gap: 40px;
  max-width: 1000px;
  margin: 0 auto;
  padding: 2rem;
}

.pile-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.pile-box {
  aspect-ratio: 1;
  padding: 1rem;
  width: 100%;
  max-width: 120px;
  text-align: center;
  border-radius: 10px;
  box-shadow: 0 3px 10px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  font-size: 0.85rem;
}

.pile-box:hover {
  transform: scale(1.05);
  box-shadow: 0 5px 15px rgba(0,0,0,0.15);
}
.pile-box.active {
  transform: scale(1.1);
  box-shadow: 0 5px 18px #007bff;
  border: 2px solid #007bff;
}

.pile-box .icon {
  font-size: 2.2rem;
  margin-bottom: 0.6rem;
}
.pile-box .label {
  font-size: 0.85rem;
  line-height: 1.4;
}
.pile-box.on {
  background-color: #e6ffec;
  border: 2px solid #52c41a;
}
.pile-box.off {
  background-color: #ffecec;
  border: 2px solid #ff5c5c;
}

@media (max-width: 768px) {
  .overview {
    padding: 2rem 1rem;
  }
  
  .pile-icons {
    grid-template-columns: repeat(3, 1fr);
    max-width: 600px;
    gap: 25px;
    padding: 1.5rem;
  }
  
  .pile-box {
    max-width: 110px;
    font-size: 0.8rem;
    padding: 0.8rem;
  }
  
  .pile-box .icon {
    font-size: 2rem;
    margin-bottom: 0.5rem;
  }
}

@media (max-width: 480px) {
  .overview {
    padding: 1.5rem 0.5rem;
  }
  
  .pile-icons {
    grid-template-columns: repeat(2, 1fr);
    max-width: 400px;
    gap: 20px;
    padding: 1rem;
  }
  
  .pile-box {
    max-width: 100px;
    font-size: 0.75rem;
    padding: 0.7rem;
  }
  
  .pile-box .icon {
    font-size: 1.8rem;
    margin-bottom: 0.4rem;
  }
}

</style>
