const TOKEN_KEY = "campus_token"
const USER_KEY = "campus_user"

const getToken = () => wx.getStorageSync(TOKEN_KEY) || ""
const getUser = () => wx.getStorageSync(USER_KEY) || null

const setAuth = ({ token, user }) => {
  wx.setStorageSync(TOKEN_KEY, token)
  wx.setStorageSync(USER_KEY, user)
}

const clearAuth = () => {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

const requireStudent = () => {
  const token = getToken()
  const user = getUser()
  if (!token || !user) {
    wx.redirectTo({ url: "/pages/login/login" })
    return null
  }
  if (user.roleId !== "5") {
    clearAuth()
    wx.showToast({ title: "仅学生账号可进入", icon: "none" })
    wx.redirectTo({ url: "/pages/login/login" })
    return null
  }
  return user
}

module.exports = {
  TOKEN_KEY,
  USER_KEY,
  getToken,
  getUser,
  setAuth,
  clearAuth,
  requireStudent
}
