import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('kfc_token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: support wrapped and direct responses
request.interceptors.response.use(
  response => {
    const res = response.data
    // If response data contains a 'code' field, handle it as wrapped data
    if (res && typeof res === 'object' && 'code' in res) {
      if (res.code !== 200) {
        ElMessage.error(res.msg || 'Request failed')
        return Promise.reject(new Error(res.msg || 'Error'))
      }
      return res.data
    }
    // Otherwise return raw data directly, such as arrays or objects
    return res
  },
  error => {
    const msg = error.response?.data?.msg || error.message || 'Network error'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
