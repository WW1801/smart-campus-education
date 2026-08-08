import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({ get: vi.fn(), push: vi.fn() }))
vi.mock('../utils/request', () => ({ default: { get: mocks.get } }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }) }))

import Dashboard from '../views/dashboard/Index.vue'

const analysisData = {
  dataOverview: {
    studentCount: 20,
    activeStudentCount: 18,
    failedCourseCount: 2,
    gradePassRate: 91.5,
    attendanceRecordCount: 100,
    abnormalAttendanceCount: 3,
    graduationAuditCount: 5,
    approvedGraduationCount: 4,
    rejectedGraduationCount: 1,
    graduationApprovalRate: 80
  },
  insights: ['两个教学班存在异常考勤记录']
}

const installResponses = ({ analysis = analysisData, warnings = [], rejectAnalysis = false, rejectWarnings = false } = {}) => {
  mocks.get.mockImplementation((url) => {
    if (url === '/semester/list') return Promise.resolve({ data: [{ semesterId: 'S1', name: '2026—2027 学年第一学期', status: 'current' }] })
    if (url === '/agent/analysis') return rejectAnalysis ? Promise.reject(new Error('analysis')) : Promise.resolve({ data: analysis })
    if (url === '/agent/warnings/students') return rejectWarnings ? Promise.reject(new Error('warnings')) : Promise.resolve({ data: { records: warnings, total: warnings.length } })
    return Promise.resolve({ data: [] })
  })
}

describe('管理员数据看板状态', () => {
  beforeEach(() => {
    mocks.get.mockReset()
    mocks.push.mockReset()
  })

  it('成功时显示真实指标和动态学期', async () => {
    installResponses()
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.text()).toContain('2026—2027 学年第一学期')
    expect(wrapper.text()).toContain('在读学生')
    expect(wrapper.text()).toContain('18')
    expect(wrapper.text()).toContain('数据摘要')
  })

  it('空数据与请求失败使用不同状态', async () => {
    installResponses({ analysis: { dataOverview: {}, insights: [] }, warnings: [] })
    const emptyWrapper = mount(Dashboard)
    await flushPromises()
    expect(emptyWrapper.text()).toContain('当前范围暂无待办')
    emptyWrapper.unmount()

    installResponses({ rejectAnalysis: true, rejectWarnings: true })
    const errorWrapper = mount(Dashboard)
    await flushPromises()
    expect(errorWrapper.text()).toContain('暂时无法取得教务台账')
    expect(errorWrapper.text()).toContain('未展示过期缓存')
  })

  it('单接口失败时保留可用内容并提示局部失败', async () => {
    installResponses({ rejectWarnings: true })
    const wrapper = mount(Dashboard)
    await flushPromises()
    expect(wrapper.text()).toContain('部分数据暂未加载')
    expect(wrapper.text()).toContain('18')
  })
})

