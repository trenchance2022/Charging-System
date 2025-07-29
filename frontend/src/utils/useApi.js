import { ref } from 'vue'
import apiService from './api'

/**
 * API调用的组合式函数
 * 统一处理加载状态、错误处理和数据管理
 */
export function useApi() {
  const loading = ref(false)
  const error = ref('')
  const data = ref(null)

  /**
   * 执行API调用
   * @param {Function} apiCall - API调用函数
   * @param {Object} options - 选项
   * @param {boolean} options.showError - 是否显示错误提示
   * @param {string} options.errorMessage - 自定义错误消息
   */
  async function execute(apiCall, options = {}) {
    const { showError = true, errorMessage = '操作失败' } = options
    
    loading.value = true
    error.value = ''
    
    try {
      const result = await apiCall()
      data.value = result
      return result
    } catch (err) {
      const message = err.message || errorMessage
      error.value = message
      
      if (showError) {
        console.error('API调用失败:', message)
      }
      
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置状态
   */
  function reset() {
    loading.value = false
    error.value = ''
    data.value = null
  }

  /**
   * 清除错误
   */
  function clearError() {
    error.value = ''
  }

  return {
    loading,
    error,
    data,
    execute,
    reset,
    clearError
  }
}

/**
 * 充电相关的组合式函数
 */
export function useCharge() {
  const { loading, error, data, execute } = useApi()

  const submitRequest = (requestData) => 
    execute(() => apiService.charge.submitRequest(requestData))

  const modifyRequest = (requestData) => 
    execute(() => apiService.charge.modifyRequest(requestData))

  const startCharging = () => 
    execute(() => apiService.charge.start())

  const stopCharging = () => 
    execute(() => apiService.charge.stop())

  const cancelCharging = () => 
    execute(() => apiService.charge.cancel())

  const getUserStatus = () => 
    execute(() => apiService.charge.getUserStatus())

  return {
    loading,
    error,
    data,
    submitRequest,
    modifyRequest,
    startCharging,
    stopCharging,
    cancelCharging,
    getUserStatus
  }
}

/**
 * 账单相关的组合式函数
 */
export function useBills() {
  const { loading, error, data, execute } = useApi()

  const getCurrentUserBills = () => 
    execute(() => apiService.bills.getCurrentUserBills())

  const getBillById = (billId) => 
    execute(() => apiService.bills.getBillById(billId))

  const getBillByNumber = (billNumber) => 
    execute(() => apiService.bills.getBillByNumber(billNumber))

  return {
    loading,
    error,
    data,
    getCurrentUserBills,
    getBillById,
    getBillByNumber
  }
}

/**
 * 认证相关的组合式函数
 */
export function useAuth() {
  const { loading, error, data, execute } = useApi()

  const login = (credentials) => 
    execute(() => apiService.auth.login(credentials))

  const register = (userData) => 
    execute(() => apiService.auth.register(userData))

  return {
    loading,
    error,
    data,
    login,
    register
  }
}

/**
 * 管理员相关的组合式函数
 */
export function useAdmin() {
  const { loading, error, data, execute } = useApi()

  const getPileStatus = () => 
    execute(() => apiService.admin.getPileStatus())

  const getPileDetail = (pileId) => 
    execute(() => apiService.admin.getPileDetail(pileId))

  const togglePileStatus = (pileId) => 
    execute(() => apiService.admin.togglePileStatus(pileId))

  const getSystemConfig = () => 
    execute(() => apiService.admin.getSystemConfig())

  const updateSystemConfig = (configData) => 
    execute(() => apiService.admin.updateSystemConfig(configData))

  const getReportStatistics = (startDate, endDate) => 
    execute(() => apiService.admin.getReportStatistics(startDate, endDate))

  const addChargingPile = (pileType) => 
    execute(() => apiService.admin.addChargingPile(pileType))

  const deleteChargingPile = (pileNumber) => 
    execute(() => apiService.admin.deleteChargingPile(pileNumber))

  return {
    loading,
    error,
    data,
    getPileStatus,
    getPileDetail,
    togglePileStatus,
    getSystemConfig,
    updateSystemConfig,
    getReportStatistics,
    addChargingPile,
    deleteChargingPile
  }
}

/**
 * 通知相关的组合式函数
 */
export function useNotifications() {
  const { loading, error, data } = useApi()

  /**
   * 创建通知SSE连接
   * @param {string} token JWT令牌
   * @returns {EventSource} SSE连接对象
   */
  function createNotificationConnection(token) {
    const url = `/api/notifications/connect?token=${token}`
    return new EventSource(url)
  }

  return {
    loading,
    error,
    data,
    createNotificationConnection
  }
} 