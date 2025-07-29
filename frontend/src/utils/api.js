import axios from 'axios'

/**
 * API服务类，统一管理所有HTTP请求
 */
class ApiService {
  constructor() {
    // 配置axios默认值
    axios.defaults.timeout = 10000
    this.setupInterceptors()
  }

  // 设置请求和响应拦截器
  setupInterceptors() {
    // 请求拦截器
    axios.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('jwt')
        if (token) {
          config.headers['Authorization'] = `Bearer ${token}`
        }
        return config
      },
      (error) => Promise.reject(error)
    )

    // 响应拦截器
    axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response && error.response.status === 401) {
          console.error('未授权，请重新登录')
          // 可以在这里添加跳转到登录页面的逻辑
        }
        return Promise.reject(error)
      }
    )
  }

  // 通用请求方法
  async request(method, url, data = null, config = {}) {
    try {
      const response = await axios({
        method,
        url,
        data,
        ...config
      })
      return response.data
    } catch (error) {
      const message = error.response?.data?.message || error.message || '请求失败'
      throw new Error(message)
    }
  }

  // GET请求
  get(url, config = {}) {
    return this.request('GET', url, null, config)
  }

  // POST请求
  post(url, data = null, config = {}) {
    return this.request('POST', url, data, config)
  }

  // PUT请求
  put(url, data = null, config = {}) {
    return this.request('PUT', url, data, config)
  }

  // DELETE请求
  delete(url, config = {}) {
    return this.request('DELETE', url, null, config)
  }

  // 认证相关API
  auth = {
    login: (credentials) => this.post('/api/login', credentials),
    register: (userData) => this.post('/api/register', userData)
  }

  // 充电相关API
  charge = {
    submitRequest: (requestData) => this.post('/api/charge/request', requestData),
    modifyRequest: (requestData) => this.post('/api/charge/modify', requestData),
    start: () => this.post('/api/charge/start'),
    stop: () => this.post('/api/charge/stop'),
    cancel: () => this.post('/api/charge/cancel'),
    getUserStatus: () => this.get('/api/charge/status/user'),
    getBatteryCapacity: () => this.get('/api/charge/battery/capacity'),
    setBatteryCapacity: (batteryCapacity) => this.post('/api/charge/battery/capacity', { batteryCapacity })
  }

  // 队列相关API
  queue = {
    getUserStatus: () => this.get('/api/queue/user')
  }

  // 账单相关API
  bills = {
    getCurrentUserBills: () => this.get('/api/bills/user/current'),
    getBillById: (billId) => this.get(`/api/bills/${billId}`),
  }

  // 电价相关API
  pricing = {
    getCurrent: () => this.get('/api/pricing/current')
  }

  // 通知相关API
  notifications = {
    connect: (token) => `/api/notifications/connect?token=${token}`
  }

  // 管理员相关API
  admin = {
    getPileStatus: () => this.get('/api/admin/piles/status'),
    getPileDetail: (pileId) => this.get(`/api/admin/pile/${pileId}`),
    togglePileStatus: (pileId) => this.post(`/api/admin/pile/${pileId}/toggle`),
    getSystemConfig: () => this.get('/api/admin/system-config'),
    updateSystemConfig: (configData) => this.post('/api/admin/system-config', configData),
    getReportStatistics: (startDate, endDate) => this.get(`/api/admin/report?startDate=${startDate}&endDate=${endDate}`),
    addChargingPile: (pileType) => this.post(`/api/admin/piles/add?pileType=${pileType}`),
    deleteChargingPile: (pileNumber) => this.delete(`/api/admin/piles/${pileNumber}`)
  }
}

// 创建单例实例
const apiService = new ApiService()

export default apiService 