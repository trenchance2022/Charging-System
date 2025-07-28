<template>
  <div>
    <h1>智能充电系统</h1>
    <button @click="fetchData">测试后端接口</button>
    <div v-if="response">
      <h3>响应数据:</h3>
      <pre>{{ JSON.stringify(response, null, 2) }}</pre>
      <p v-if="response.data">{{ response.data }}</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'App',
  data() {
    return {
      response: null
    }
  },
  methods: {
    async fetchData() {
      try {
        const response = await axios.get('/api/hello')
        this.response = response.data
      } catch (err) {
        console.error('请求出错:', err)
      }
    }
  }
}
</script>
