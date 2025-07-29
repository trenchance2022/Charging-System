<template>
  <div class="register-page">
    <div class="register-box">
      <h2 class="title">注册</h2>
      <form @submit.prevent="handleRegister" class="form">
        <input v-model="username" placeholder="用户名" class="input" />
        <input v-model="password" type="password" placeholder="密码" class="input" />
        <input v-model="confirmPassword" type="password" placeholder="确认密码" class="input" />

        <div class="user-type">
          <label>
            <input type="radio" value="user" v-model="userType" /> 普通用户
          </label>
          <label>
            <input type="radio" value="admin" v-model="userType" /> 管理员
          </label>
        </div>

        <button type="submit" class="register-button">注册</button>
        <p class="login-link" @click="goToLogin">返回登录</p>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="success" class="success">{{ success }}</p>
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
const confirmPassword = ref('');
const userType = ref('user');
const error = ref('');
const success = ref('');
const router = useRouter();

// 注册处理函数
async function handleRegister() {
  // 验证输入
  if (!username.value || !password.value || !confirmPassword.value) {
    error.value = '所有字段都不能为空';
    return;
  }
  
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致';
    return;
  }
  
  try {
    const response = await apiService.auth.register({
      username: username.value,
      password: password.value,
      type: userType.value,
    });
    
    success.value = response.message;
    setTimeout(() => router.push('/login'), 2000); // 注册成功后跳转登录页
  } catch (err) {
    error.value = err.message || '注册失败';
  }
}

function goToLogin() {
  router.push('/login');
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: url('../assets/bg.png') no-repeat center center fixed;
  background-size: cover;
}

.register-box {
  background-color: rgba(255, 255, 255, 0.9);
  padding: 2rem;
  width: 320px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  text-align: center;
}

.title {
  font-size: 1.8rem;
  font-weight: bold;
  margin-bottom: 1.5rem;
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
}

.register-button {
  width: 100%;
  padding: 0.6rem;
  background-color: #67c23a;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}
.register-button:hover {
  background-color: #85ce61;
}

.login-link {
  margin-top: 1rem;
  color: #409eff;
  cursor: pointer;
  text-decoration: underline;
}
.error {
  color: red;
  margin-top: 0.5rem;
}
.success {
  color: green;
  margin-top: 0.5rem;
}
</style>
