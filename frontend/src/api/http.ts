import axios from 'axios'

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    // 统一解包后端 Result 包裹 {code, message, data}，返回内层 data；
    // 裸响应（如 /health 返回 {status:"ok"}）原样透传。
    if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
      return body.data
    }
    return body
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export function getHealth(): Promise<{ status: string }> {
  return http.get('/health')
}
