const request = require("../../utils/request")
const { requireStudent, clearAuth } = require("../../utils/auth")

Page({
  data: {
    account: {},
    profile: {},
    loading: false,
    error: "",
    savingPassword: false,
    passwordForm: {
      oldPassword: "",
      newPassword: "",
      confirmPassword: ""
    }
  },

  onShow() {
    if (!requireStudent()) return
    this.loadProfile()
  },

  async loadProfile() {
    this.setData({ loading: true, error: "" })
    try {
      const res = await request({ url: "/account/me" })
      if (res.data && res.data.profileType !== "student") {
        clearAuth()
        wx.showToast({ title: "仅学生账号可进入", icon: "none" })
        wx.redirectTo({ url: "/pages/login/login" })
        return
      }
      this.setData({
        account: (res.data && res.data.user) || {},
        profile: (res.data && res.data.profile) || {}
      })
    } catch (error) {
      this.setData({ error: error.message || "个人信息加载失败" })
    } finally {
      this.setData({ loading: false })
    }
  },

  onOldPasswordInput(event) {
    this.setData({ "passwordForm.oldPassword": event.detail.value })
  },

  onNewPasswordInput(event) {
    this.setData({ "passwordForm.newPassword": event.detail.value })
  },

  onConfirmPasswordInput(event) {
    this.setData({ "passwordForm.confirmPassword": event.detail.value })
  },

  validatePassword() {
    const form = this.data.passwordForm
    if (!form.oldPassword) return "请输入原密码"
    if (!form.newPassword) return "请输入新密码"
    if (form.newPassword.length < 8 || form.newPassword.length > 64) return "新密码需为 8 至 64 位"
    if (!/[A-Za-z]/.test(form.newPassword) || !/\d/.test(form.newPassword)) return "新密码需同时包含字母和数字"
    if (form.newPassword !== form.confirmPassword) return "两次新密码不一致"
    return ""
  },

  async changePassword() {
    const error = this.validatePassword()
    if (error) {
      wx.showToast({ title: error, icon: "none" })
      return
    }
    this.setData({ savingPassword: true })
    try {
      await request({
        url: "/account/password",
        method: "PUT",
        data: {
          oldPassword: this.data.passwordForm.oldPassword,
          newPassword: this.data.passwordForm.newPassword
        }
      })
      wx.showToast({ title: "密码已修改", icon: "success" })
      this.setData({ passwordForm: { oldPassword: "", newPassword: "", confirmPassword: "" } })
    } finally {
      this.setData({ savingPassword: false })
    }
  },

  logout() {
    wx.showModal({
      title: "退出登录",
      content: "确认退出当前账号？",
      success: (result) => {
        if (!result.confirm) return
        clearAuth()
        wx.redirectTo({ url: "/pages/login/login" })
      }
    })
  }
})
