/**
 * 请求工具模块，负责封装 Axios 请求与拦截逻辑。
 */
import axios from 'axios'
import store from '../store'
import router from '../router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const statusMessages = {
  400: '请求参数不正确，请检查填写内容',
  403: '当前账号没有执行此操作的权限',
  404: '请求的数据或接口不存在',
  429: '操作过于频繁，请稍后重试',
  500: '服务暂时不可用，请稍后重试'
}

const logoutAndReturnToLogin = () => {
  store.dispatch('logout')
  if (router.currentRoute.value.path !== '/') router.push('/')
}

// 为请求统一附加认证信息
request.interceptors.request.use(config => {
  const token = store.state.token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 统一处理响应结果与异常
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      const message = res.message || statusMessages[res.code] || '请求失败'
      if (!response.config.skipErrorMessage || res.code === 401) ElMessage.error(message)
      if (res.code === 401) logoutAndReturnToLogin()
      const businessError = new Error(message)
      businessError.code = res.code
      businessError.response = response
      return Promise.reject(businessError)
    }
    return res
  },
  error => {
    const status = error.response?.status
    if (status === 401) {
      logoutAndReturnToLogin()
      ElMessage.error('登录已过期，请重新登录')
    } else if (!error.config?.skipErrorMessage) {
      ElMessage.error(error.response?.data?.message || statusMessages[status] || (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络连接失败'))
    }
    return Promise.reject(error)
  }
)

export default request
