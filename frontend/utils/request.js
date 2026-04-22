import axios from 'axios'
import store from '../store'
import router from '../router'
import { ElMessage } from 'element-plus'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
})

request.interceptors.request.use(config => {
    const token = store.state.token
    if (token) {
        config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
})

request.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            if (res.code === 401) {
                store.dispatch('logout')
                router.push('/')
            }
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        if (error.response) {
            const status = error.response.status
            if (status === 401) {
                store.dispatch('logout')
                router.push('/')
                ElMessage.error('登录已过期，请重新登录')
            } else {
                ElMessage.error(error.response.data?.message || '网络错误')
            }
        } else {
            ElMessage.error('网络连接失败')
        }
        return Promise.reject(error)
    }
)

export default request
