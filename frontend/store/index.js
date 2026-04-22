import { createStore } from 'vuex'

const store = createStore({
  state: {
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    token: localStorage.getItem('token') || '',
    menuList: []
  },
  mutations: {
    setUser(state, user) {
      state.user = user
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
      } else {
        localStorage.removeItem('user')
      }
    },
    setToken(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    setMenuList(state, menuList) {
      state.menuList = menuList
    },
    logout(state) {
      state.user = null
      state.token = ''
      state.menuList = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  },
  actions: {
    login({ commit }, { user, token }) {
      commit('setUser', user)
      commit('setToken', token)
    },
    logout({ commit }) {
      commit('logout')
    }
  },
  getters: {
    isLoggedIn: state => !!state.token,
    currentUser: state => state.user,
    roleId: state => state.user?.roleId || '',
    permissions: state => state.user?.permissions || [],
    hasPermission: (state) => (permission) => {
      return state.user?.permissions?.includes(permission) || false
    }
  }
})

export default store
