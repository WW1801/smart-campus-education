import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  warning: vi.fn(),
  store: { state: { user: null } }
}))

vi.mock('../utils/request', () => ({ default: { get: mocks.get } }))
vi.mock('vuex', () => ({ useStore: () => mocks.store }))
vi.mock('element-plus', async (importOriginal) => ({
  ...(await importOriginal()),
  ElMessage: { warning: mocks.warning, success: vi.fn(), error: vi.fn() }
}))

import GradeInput from '../views/grade/Input.vue'

const semesters = [
  { semesterId: 'FUTURE', name: '2030-2031学年第二学期', startDate: '2031-02-20', status: 'planned' },
  { semesterId: 'CURRENT', name: '2026-2027学年第一学期', startDate: '2026-09-01', status: 'current' },
  { semesterId: 'PAST', name: '2025-2026学年第二学期', startDate: '2026-02-20', status: 'closed' }
]

const mountPage = async () => {
  const wrapper = mount(GradeInput, { global: { stubs: { PageErrorState: true } } })
  await flushPromises()
  return wrapper
}

describe('成绩录入筛选', () => {
  beforeEach(() => {
    mocks.get.mockReset()
    mocks.warning.mockReset()
    mocks.store.state.user = null
    mocks.get.mockImplementation(url => {
      if (url === '/semester/list') return Promise.resolve({ data: semesters })
      if (url === '/course/list') return Promise.resolve({ data: [{ courseId: 'C1', name: '高等数学' }] })
      return Promise.resolve({ data: [] })
    })
  })

  it('默认显示全部学期，不自动请求课程或成绩名单，并按开始日期倒序展示', async () => {
    const wrapper = await mountPage()

    expect(wrapper.vm.searchForm.semesterId).toBe('')
    expect(wrapper.vm.courseOptions).toEqual([])
    expect(wrapper.vm.semesterOptions.map(item => item.semesterId)).toEqual(['FUTURE', 'CURRENT', 'PAST'])
    expect(wrapper.vm.coursePlaceholder).toBe('请选择课程')
    expect(mocks.get).not.toHaveBeenCalledWith('/schedule/query/by-teacher', expect.anything())
    expect(mocks.get).not.toHaveBeenCalledWith('/schedule/list', expect.anything())
    expect(mocks.get).not.toHaveBeenCalledWith('/grade/roster', expect.anything())
  })

  it('教师选择学期后仅加载本人课程，且不自动查询成绩名单', async () => {
    mocks.store.state.user = { relatedId: 'T1', roleId: '4' }
    mocks.get.mockImplementation((url, config) => {
      if (url === '/semester/list') return Promise.resolve({ data: semesters })
      if (url === '/course/list') return Promise.resolve({ data: [{ courseId: 'C1', name: '高等数学' }] })
      if (url === '/schedule/query/by-teacher') {
        expect(config.params).toEqual({ teacherId: 'T1', semesterId: 'CURRENT' })
        return Promise.resolve({ data: [{ scheduleId: 'SC1', courseId: 'C1', teacherId: 'T1', semesterId: 'CURRENT', classId: 'CL1' }] })
      }
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountPage()

    wrapper.vm.searchForm.semesterId = 'CURRENT'
    await wrapper.vm.handleSemesterChange()

    expect(wrapper.vm.searchForm.courseContextId).toBe('')
    expect(wrapper.vm.courseOptions.map(item => item.courseId)).toEqual(['C1'])
    expect(wrapper.vm.coursePlaceholder).toBe('请选择课程')
    expect(mocks.get).not.toHaveBeenCalledWith('/grade/roster', expect.anything())
  })

  it('未选择课程时禁止查询并给出提示，不发送成绩名单请求', async () => {
    mocks.store.state.user = { relatedId: 'T1', roleId: '4' }
    mocks.get.mockImplementation((url) => {
      if (url === '/semester/list') return Promise.resolve({ data: semesters })
      if (url === '/course/list') return Promise.resolve({ data: [{ courseId: 'C1', name: '高等数学' }] })
      if (url === '/schedule/query/by-teacher') return Promise.resolve({ data: [{ scheduleId: 'SC1', courseId: 'C1', teacherId: 'T1', semesterId: 'CURRENT', classId: 'CL1' }] })
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountPage()
    wrapper.vm.searchForm.semesterId = 'CURRENT'
    await wrapper.vm.handleSemesterChange()

    expect(wrapper.vm.canQuery).toBe(false)
    await wrapper.vm.handleQuery()
    expect(mocks.warning).toHaveBeenCalledWith('请选择课程')
    expect(mocks.get).not.toHaveBeenCalledWith('/grade/roster', expect.anything())
  })

  it('管理员在所选学期加载全部排课课程，不携带教师筛选条件', async () => {
    mocks.store.state.user = { relatedId: null, roleId: '1' }
    mocks.get.mockImplementation((url, config) => {
      if (url === '/semester/list') return Promise.resolve({ data: semesters })
      if (url === '/course/list') return Promise.resolve({ data: [{ courseId: 'C2', name: '数据库' }] })
      if (url === '/schedule/list') {
        expect(config.params).toEqual({ semesterId: 'CURRENT' })
        return Promise.resolve({ data: [{ scheduleId: 'SC2', courseId: 'C2', teacherId: 'T2', semesterId: 'CURRENT', classId: 'CL2' }] })
      }
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountPage()
    wrapper.vm.searchForm.semesterId = 'CURRENT'
    await wrapper.vm.handleSemesterChange()

    expect(wrapper.vm.courseOptions.map(item => item.courseId)).toEqual(['C2'])
    expect(mocks.get).not.toHaveBeenCalledWith('/schedule/query/by-teacher', expect.anything())
  })

  it('按唯一排课上下文查询，班级不会混合且名单请求携带完整参数', async () => {
    mocks.store.state.user = { relatedId: 'T1', roleId: '4' }
    mocks.get.mockImplementation((url) => {
      if (url === '/semester/list') return Promise.resolve({ data: semesters })
      if (url === '/course/list') return Promise.resolve({ data: [{ courseId: 'C1', name: '软件工程' }] })
      if (url === '/teacher/list') return Promise.resolve({ data: [{ teacherId: 'T1', name: '张老师' }] })
      if (url === '/class/list') return Promise.resolve({ data: [
        { classId: 'CL25', name: '1班', grade: '2025级' },
        { classId: 'CL24', name: '2班', grade: '2024级' }
      ] })
      if (url === '/major/list') return Promise.resolve({ data: [] })
      if (url === '/schedule/query/by-teacher') return Promise.resolve({ data: [
        { scheduleId: 'SC25', courseId: 'C1', teacherId: 'T1', semesterId: 'CURRENT', classId: 'CL25' },
        { scheduleId: 'SC24', courseId: 'C1', teacherId: 'T1', semesterId: 'CURRENT', classId: 'CL24' }
      ] })
      if (url === '/grade/roster') return Promise.resolve({ data: [{ studentId: 'S25' }] })
      return Promise.resolve({ data: [] })
    })
    const wrapper = await mountPage()
    wrapper.vm.searchForm.semesterId = 'CURRENT'
    await wrapper.vm.handleSemesterChange()
    wrapper.vm.searchForm.courseContextId = 'CURRENT|C1|T1'
    await wrapper.vm.handleCourseChange()

    expect(wrapper.vm.classOptions.map(item => item.scheduleId)).toEqual(['SC25', 'SC24'])
    wrapper.vm.searchForm.scheduleId = 'SC25'
    await wrapper.vm.handleQuery()
    expect(mocks.get).toHaveBeenCalledWith('/grade/roster', expect.objectContaining({
      params: { scheduleId: 'SC25', semesterId: 'CURRENT', courseId: 'C1', classId: 'CL25', teacherId: 'T1' },
      skipErrorMessage: true
    }))
    expect(wrapper.vm.tableData).toEqual([{ studentId: 'S25' }])
  })
})
