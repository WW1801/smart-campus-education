const request = require("../../utils/request")
const { requireStudent } = require("../../utils/auth")

Page({
  data: {
    loggedIn: false,
    user: null,
    semesters: [],
    semesterOptions: [],
    semesterIndex: 0,
    currentSemesterName: "",
    schedules: [],
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
      const semesters = Array.isArray(res.data) ? res.data : []
      const currentIndex = Math.max(0, semesters.findIndex(item => ["current", "active"].includes(item.status)))
      this.setData({
        semesters,
        semesterOptions: semesters,
        semesterIndex: currentIndex,
        currentSemesterName: semesters[currentIndex] ? semesters[currentIndex].name : ""
      })
    } catch (error) {
      this.setData({ semesters: [], semesterOptions: [], currentSemesterName: "" })
    }
    this.loadSchedule()
  },

  onSemesterChange(event) {
    const index = Number(event.detail.value)
    const semester = this.data.semesters[index]
    this.setData({ semesterIndex: index, currentSemesterName: semester ? semester.name : "" })
    this.loadSchedule()
  },

  async loadSchedule() {
    const user = this.data.user || requireStudent()
    if (!user) return
    const semester = this.data.semesters[this.data.semesterIndex]
    this.setData({ loading: true, error: "" })
    try {
      const res = await request({
        url: "/selection/timetable",
        data: {
          studentId: user.relatedId,
          semesterId: semester ? semester.semesterId : ""
        }
      })
      this.setData({ schedules: Array.isArray(res.data) ? res.data : [] })
    } catch (error) {
      this.setData({ schedules: [], error: error.message || "课表加载失败" })
    } finally {
      this.setData({ loading: false })
    }
  }
})
