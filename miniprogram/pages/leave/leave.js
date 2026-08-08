const request = require("../../utils/request")
const { requireStudent } = require("../../utils/auth")

const leaveTypes = [
  { label: "病假", value: "sick" },
  { label: "事假", value: "personal" },
  { label: "公假", value: "official" },
  { label: "其他", value: "other" }
]

const statusMap = {
  pending: { text: "待审批", className: "status-warn" },
  approved: { text: "已通过", className: "status-ok" },
  rejected: { text: "已驳回", className: "status-danger" },
  cancelled: { text: "已取消", className: "status-danger" }
}

Page({
  data: {
    activeTab: "form",
    user: null,
    leaveTypes,
    leaveTypeIndex: 0,
    semesters: [],
    semesterOptions: [],
    semesterIndex: 0,
    currentSemesterName: "",
    courseOptions: [{ name: "不关联课程", courseId: "" }],
    courseIndex: 0,
    selectedCourseName: "不关联课程",
    form: { startDate: "", endDate: "", reason: "" },
    submitting: false,
    records: [],
    recordsLoading: false,
    recordsError: ""
  },

  onShow() {
    const user = requireStudent()
    if (!user) return
    this.setData({ user })
    this.loadSemesters()
    this.loadRecords()
  },

  showForm() {
    this.setData({ activeTab: "form" })
  },

  showRecords() {
    this.setData({ activeTab: "records" })
    this.loadRecords()
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
    this.loadCourses()
  },

  async loadCourses() {
    const semester = this.data.semesters[this.data.semesterIndex]
    const user = this.data.user
    if (!user) return
    try {
      const res = await request({
        url: "/selection/timetable",
        data: {
          studentId: user.relatedId,
          semesterId: semester ? semester.semesterId : ""
        },
        showError: false
      })
      const seen = {}
      const courses = [{ name: "不关联课程", courseId: "" }]
      ;(Array.isArray(res.data) ? res.data : []).forEach(item => {
        if (item.courseId && !seen[item.courseId]) {
          seen[item.courseId] = true
          courses.push({ name: item.courseName || item.courseId, courseId: item.courseId })
        }
      })
      this.setData({ courseOptions: courses, courseIndex: 0, selectedCourseName: courses[0].name })
    } catch (error) {
      this.setData({ courseOptions: [{ name: "不关联课程", courseId: "" }], courseIndex: 0, selectedCourseName: "不关联课程" })
    }
  },

  onLeaveTypeChange(event) {
    this.setData({ leaveTypeIndex: Number(event.detail.value) })
  },

  onSemesterChange(event) {
    const index = Number(event.detail.value)
    const semester = this.data.semesters[index]
    this.setData({ semesterIndex: index, currentSemesterName: semester ? semester.name : "" })
    this.loadCourses()
  },

  onCourseChange(event) {
    const index = Number(event.detail.value)
    const course = this.data.courseOptions[index]
    this.setData({ courseIndex: index, selectedCourseName: course ? course.name : "不关联课程" })
  },

  onStartDateChange(event) {
    this.setData({ "form.startDate": event.detail.value })
  },

  onEndDateChange(event) {
    this.setData({ "form.endDate": event.detail.value })
  },

  onReasonInput(event) {
    this.setData({ "form.reason": event.detail.value })
  },

  validateForm() {
    const form = this.data.form
    if (!this.data.semesters[this.data.semesterIndex]) return "请选择学期"
    if (!form.startDate) return "请选择开始日期"
    if (!form.endDate) return "请选择结束日期"
    if (form.endDate < form.startDate) return "结束日期不能早于开始日期"
    if (!form.reason.trim()) return "请填写请假原因"
    return ""
  },

  async submitLeave() {
    const error = this.validateForm()
    if (error) {
      wx.showToast({ title: error, icon: "none" })
      return
    }
    const semester = this.data.semesters[this.data.semesterIndex]
    const course = this.data.courseOptions[this.data.courseIndex]
    this.setData({ submitting: true })
    try {
      await request({
        url: "/leave-requests",
        method: "POST",
        data: {
          studentId: this.data.user.relatedId,
          semesterId: semester.semesterId,
          courseId: course && course.courseId ? course.courseId : "",
          leaveType: this.data.leaveTypes[this.data.leaveTypeIndex].value,
          startDate: this.data.form.startDate,
          endDate: this.data.form.endDate,
          reason: this.data.form.reason.trim()
        }
      })
      wx.showToast({ title: "已提交", icon: "success" })
      this.setData({ form: { startDate: "", endDate: "", reason: "" }, activeTab: "records" })
      this.loadRecords()
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadRecords() {
    const user = this.data.user || requireStudent()
    if (!user) return
    this.setData({ recordsLoading: true, recordsError: "" })
    try {
      const res = await request({ url: "/leave-requests/my", data: { page: 1, limit: 50 } })
      const records = (((res.data || {}).records) || []).map(item => {
        const status = statusMap[item.status] || { text: item.status || "-", className: "status-warn" }
        const type = leaveTypes.find(option => option.value === item.leaveType)
        return Object.assign({}, item, {
          statusText: status.text,
          statusClass: status.className,
          leaveTypeText: type ? type.label : item.leaveType
        })
      })
      this.setData({ records })
    } catch (error) {
      this.setData({ records: [], recordsError: error.message || "请假记录加载失败" })
    } finally {
      this.setData({ recordsLoading: false })
    }
  },

  cancelLeave(event) {
    const requestId = event.currentTarget.dataset.id
    wx.showModal({
      title: "取消申请",
      content: "确认取消这条待审批请假申请？",
      success: async (result) => {
        if (!result.confirm) return
        await request({ url: `/leave-requests/${requestId}/cancel`, method: "PUT" })
        wx.showToast({ title: "已取消", icon: "success" })
        this.loadRecords()
      }
    })
  }
})
