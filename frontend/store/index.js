/**
 * 状态管理模块，负责维护全局用户与权限状态。
 */
import { createStore } from 'vuex'

// 读取本地缓存的用户信息
const readStoredUser = () => {
  const rawUser = localStorage.getItem('user')
  if (!rawUser) {
    return null
  }

  try {
    return JSON.parse(rawUser)
  } catch {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return null
  }
}

// 读取本地缓存的认证信息
const readStoredAuth = () => {
  const user = readStoredUser()
  const token = localStorage.getItem('token') || ''

  if (!user || !token || !user.roleId) {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return { user: null, token: '' }
  }

  return { user, token }
}

const storedAuth = readStoredAuth()

const store = createStore({
  state: {
    user: storedAuth.user,
    token: storedAuth.token,
    menuList: []
  },
  mutations: {
    // 设置用户
    setUser(state, user) {
      state.user = user
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    },
    // 设置令牌
    setToken(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    // 设置菜单列表
    setMenuList(state, menuList) {
      state.menuList = menuList
    },
    // 清空登录状态
    logout(state) {
      state.user = null
      state.token = ''
      state.menuList = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  },
  actions: {
    // 处理登录
    login({ commit }, { user, token }) {
      commit('setUser', user)
      commit('setToken', token)
    },
    // 清空登录状态
    logout({ commit }) {
      commit('logout')
    }
  },
  getters: {
    // 判断当前是否已登录
    isLoggedIn: state => !!state.token,
    // 返回当前用户
    currentUser: state => state.user,
    // 返回当前角色编号
    roleId: state => state.user?.roleId || '',
    // 返回当前权限列表
    permissions: state => state.user?.permissions || [],
    // 判断当前用户是否具备指定权限
    hasPermission: (state) => (permission) => {
      return state.user?.permissions?.includes(permission) || false
    }
  }
})

export default store
