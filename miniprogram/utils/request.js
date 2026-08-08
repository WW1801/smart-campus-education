const { getToken, clearAuth } = require("./auth")

const statusMessages = {
  400: "请求参数不正确",
  401: "登录已过期，请重新登录",
  403: "当前账号无权操作",
  404: "接口或数据不存在",
  500: "服务暂时不可用"
}

const request = ({ url, method = "GET", data = {}, skipAuth = false, showError = true }) => {
  const app = getApp()
  const headers = {
    "content-type": "application/json"
  }
  const token = getToken()
  if (token && !skipAuth) {
    headers.Authorization = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseURL}${url}`,
      method,
      data,
      header: headers,
      success: (response) => {
        const body = response.data || {}
        const code = body.code || response.statusCode
        if (response.statusCode === 401 || code === 401) {
          clearAuth()
          if (showError) wx.showToast({ title: body.message || statusMessages[401], icon: "none" })
          wx.redirectTo({ url: "/pages/login/login" })
          reject(body)
          return
        }
        if (response.statusCode >= 400 || code !== 200) {
          if (showError) wx.showToast({ title: body.message || statusMessages[code] || "请求失败", icon: "none" })
          reject(body)
          return
        }
        resolve(body)
      },
      fail: (error) => {
        if (showError) wx.showToast({ title: "无法连接后端服务", icon: "none" })
        reject(error)
      }
    })
  })
}

module.exports = request
