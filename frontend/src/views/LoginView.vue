<template>
  <div class="login-page">
    <div class="login-box">
      <h2 class="title">登录</h2>
      <form @submit.prevent="handleLogin" class="form">
        <input v-model="username" placeholder="用户名" class="input" />
        <input v-model="password" type="password" placeholder="密码" class="input" />

        <div class="user-type">
          <label>
            <input type="radio" value="user" v-model="userType" /> 普通用户
          </label>
          <label>
            <input type="radio" value="admin" v-model="userType" /> 管理员
          </label>
        </div>

        <button type="submit" class="login-button">登录</button>
        <p class="register-link" @click="goToRegister">注册</p>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </div>
  </div>
</template>


<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiService from '../utils/api';

const username = ref('');
const password = ref('');
const userType = ref('user');
const error = ref('');
const router = useRouter();


// 登录处理函数
async function handleLogin() {
  // 如果用户名或密码为空
  if (!username.value || !password.value) {
    error.value = '用户名和密码不能为空';
    return;
  }
  
  try {
    const response = await apiService.auth.login({
      username: username.value,
      password: password.value,
      type: userType.value,
    });

    // 登录成功后，读取后端返回 JWT（令牌）和类型
    const token = response.token;
    const type = response.type;
    // 本地存储 token
    localStorage.setItem('jwt', token);
    // 跳转到对应的控制台页面
    if (type === 'admin') {
      router.push('/admin/console');
    } else {
      router.push('/user/console');
    }
  } catch (err) {
    error.value = err.message || '登录失败';
  }
}

function goToRegister() {
  router.push('/register');
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: url('../assets/bg.png') no-repeat center center fixed;
  background-size: cover;
}

.login-box {
  background-color: rgba(255, 255, 255, 0.9); /* 半透明白色背景 */
  padding: 2rem;
  width: 320px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  text-align: center;
  backdrop-filter: blur(8px);
}

.title {
  font-size: 1.8rem;
  font-weight: bold;
  margin-bottom: 1.5rem;
  color: #333;
}

.input {
  width: 100%;
  margin-bottom: 1rem;
  padding: 0.6rem;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 6px;
}

.user-type {
  display: flex;
  justify-content: center;
  margin-bottom: 1rem;
}

.user-type label {
  margin: 0 1rem;
  font-size: 0.95rem;
}

.login-button {
  width: 100%;
  padding: 0.6rem;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.login-button:hover {
  background-color: #66b1ff;
}

.register-link {
  margin-top: 1rem;
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}

.error {
  color: red;
  margin-top: 0.5rem;
}
</style>

