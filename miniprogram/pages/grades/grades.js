const request = require("../../utils/request")
const { requireStudent } = require("../../utils/auth")

const statusMap = {
  submitted: { text: "待审核", className: "status-warn" },
  approved: { text: "已通过", className: "status-ok" },
  rejected: { text: "已驳回", className: "status-danger" },
  draft: { text: "草稿", className: "status-warn" }
}

Page({
  data: {
    loggedIn: false,
    user: null,
    semesters: [],
    semesterOptions: [{ name: "全部学期", semesterId: "" }],
    semesterIndex: 0,
    currentSemesterName: "全部学期",
    grades: [],
    gpa: {},
    loading: false,
    error: ""
  },

  onShow() {
    const user = requireStudent()
    if (!user) {
      this.setData({ loggedIn: false })
      return
    }
    this.setData({ loggedIn: true, user })
    this.loadSemesters()
  },

  async loadSemesters() {
    try {
      const res = await request({ url: "/semester/list", showError: false })
      const semesters = [{ name: "全部学期", semesterId: "" }].concat(Array.isArray(res.data) ? res.data : [])
      this.setData({ semesters, semesterOptions: semesters })
    } catch (error) {
      this.setData({ semesters: [{ name: "全部学期", semesterId: "" }], semesterOptions: [{ name: "全部学期", semesterId: "" }] })
    }
    this.loadGrades()
  },

  onSemesterChange(event) {
    const index = Number(event.detail.value)
    const semester = this.data.semesters[index]
    this.setData({ semesterIndex: index, currentSemesterName: semester ? semester.name : "全部学期" })
    this.loadGrades()
  },

  async loadGrades() {
    const user = this.data.user || requireStudent()
    if (!user) return
    const semester = this.data.semesters[this.data.semesterIndex] || {}
    this.setData({ loading: true, error: "" })
    try {
      const [pageRes, gpaRes] = await Promise.all([
        request({
          url: "/grade/page",
          data: {
            current: 1,
            size: 100,
            studentId: user.relatedId,
            semesterId: semester.semesterId || ""
          }
        }),
        request({
          url: `/grade/gpa/${user.relatedId}`,
          data: {
            semesterId: semester.semesterId || ""
          },
          showError: false
        })
      ])
      const records = ((pageRes.data && pageRes.data.records) || []).map(item => {
        const status = statusMap[item.status] || { text: item.status || "-", className: "status-warn" }
        return Object.assign({}, item, { statusText: status.text, statusClass: status.className })
      })
      this.setData({ grades: records, gpa: gpaRes.data || {} })
    } catch (error) {
      this.setData({ grades: [], gpa: {}, error: error.message || "成绩加载失败" })
    } finally {
      this.setData({ loading: false })
    }
  }
})
