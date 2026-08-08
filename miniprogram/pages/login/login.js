const request = require("../../utils/request")
const { setAuth, clearAuth } = require("../../utils/auth")

Page({
  data: {
    username: "",
    password: "",
    loading: false
  },

  onLoad() {
    clearAuth()
  },

  onUsernameInput(event) {
    this.setData({ username: event.detail.value })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  async handleLogin() {
    const username = this.data.username.trim()
    const password = this.data.password
    if (!username) {
      wx.showToast({ title: "请输入账号", icon: "none" })
      return
    }
    if (!password) {
      wx.showToast({ title: "请输入密码", icon: "none" })
      return
    }

    this.setData({ loading: true })
    try {
      const res = await request({
        url: "/login",
        method: "POST",
        data: { username, password },
        skipAuth: true
      })
      const token = res.data && res.data.token
      const user = res.data && res.data.user
      if (!token || !user) {
        wx.showToast({ title: "登录响应缺少必要字段", icon: "none" })
        return
      }
      if (user.roleId !== "5") {
        wx.showToast({ title: "仅学生账号可使用小程序", icon: "none" })
        return
      }
      setAuth({ token, user })
      wx.switchTab({ url: "/pages/schedule/schedule" })
    } catch (error) {
      if (!error || !error.message) return
    } finally {
      this.setData({ loading: false })
    }
  }
})
