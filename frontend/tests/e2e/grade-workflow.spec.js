import { expect, test } from '@playwright/test'

const ok = data => ({ code: 200, message: '操作成功', data })
const reply = (route, body, status = 200) => route.fulfill({
  status,
  contentType: 'application/json',
  body: JSON.stringify(body)
})

const installSession = async (page, user) => {
  await page.addInitScript(sessionUser => {
    localStorage.setItem('token', 'e2e-grade-token')
    localStorage.setItem('user', JSON.stringify(sessionUser))
  }, user)
}

const selectGradeFilters = async page => {
  const filters = page.locator('.search-bar .el-select')
  const choose = async (index, optionIndex, optionCount) => {
    await expect(filters).toHaveCount(3)
    const filter = filters.nth(index)
    const input = filter.getByRole('combobox')
    await expect(input).toHaveCount(1)
    await filter.click()
    const options = page.getByRole('option').filter({ visible: true })
    await expect(options).toHaveCount(optionCount)
    await options.nth(optionIndex).click()
  }
  await choose(0, 1, 2)
  await choose(1, 0, 1)
  await choose(2, 0, 1)
  await page.getByRole('button', { name: '查询' }).click()
}

test('教师单条成绩校验、失败保留和重试成功', async ({ page }) => {
  await installSession(page, { userId: 'UT', username: 'T015', name: '黄老师', roleId: '4', relatedId: 'T015' })
  const consoleErrors = []
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  let submitCalls = 0
  let saved = false
  const submittedBodies = []

  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    if (path.endsWith('/semester/list')) return reply(route, ok([{ semesterId: 'SEM1', name: '2026-2027学年第一学期', status: 'current' }]))
    if (path.endsWith('/course/list')) return reply(route, ok([{ courseId: 'C1', name: '软件测试' }]))
    if (path.endsWith('/schedule/query/by-teacher')) {
      expect(url.searchParams.get('teacherId')).toBe('T015')
      expect(url.searchParams.get('semesterId')).toBe('SEM1')
      return reply(route, ok([{ scheduleId: 'SC1', courseId: 'C1', teacherId: 'T015', semesterId: 'SEM1', classId: 'CL1' }]))
    }
    if (path.endsWith('/grade/roster')) {
      expect(route.request().method()).toBe('GET')
      expect(url.searchParams.get('courseId')).toBe('C1')
      expect(url.searchParams.get('semesterId')).toBe('SEM1')
      expect(url.searchParams.get('teacherId')).toBe('T015')
      expect(url.searchParams.get('scheduleId')).toBe('SC1')
      expect(url.searchParams.get('classId')).toBe('CL1')
      return reply(route, ok([{
        gradeId: saved ? 'G1' : undefined,
        studentId: 'S016', studentName: '李贺', courseId: 'C1', courseName: '软件测试',
        semesterId: 'SEM1', teacherId: 'T015', usualScore: saved ? 90 : null,
        examScore: saved ? 91 : null, totalScore: saved ? 90.7 : null, status: saved ? 'submitted' : 'draft'
      }]))
    }
    if (path.endsWith('/grade') && route.request().method() === 'POST') {
      submitCalls++
      submittedBodies.push(route.request().postDataJSON())
      if (submitCalls === 1) {
        return reply(route, { code: 500, message: '成绩状态配置尚未升级，请系统管理员执行 20260726_grade_status.sql 后重试', data: null })
      }
      saved = true
      return reply(route, ok(null))
    }
    return reply(route, ok([]))
  })

  await page.goto('/home/grade/input')
  await selectGradeFilters(page)
  const row = page.locator('.el-table__body tr').filter({ hasText: 'S016' })
  await expect(row).toHaveCount(1)
  await row.getByRole('button', { name: '提交' }).click()
  await expect(page.getByText('平时成绩和考试成绩至少填写一项')).toBeVisible()
  expect(submitCalls).toBe(0)

  const scoreInputs = row.getByRole('spinbutton')
  await expect(scoreInputs).toHaveCount(2)
  await scoreInputs.nth(0).fill('90')
  await scoreInputs.nth(1).fill('91')
  await row.getByRole('button', { name: '提交' }).click()
  await expect(page.getByTestId('grade-submit-error')).toHaveText('成绩状态配置尚未升级，请系统管理员执行 20260726_grade_status.sql 后重试')
  await expect(scoreInputs.nth(0)).toHaveValue('90.0')
  await expect(scoreInputs.nth(1)).toHaveValue('91.0')

  await row.getByRole('button', { name: '提交' }).click()
  await expect(page.getByText('已提交（待审核）')).toBeVisible()
  expect(submittedBodies[1]).toEqual({
    studentId: 'S016', courseId: 'C1', semesterId: 'SEM1', teacherId: 'T015', usualScore: 90, examScore: 91
  })
  expect(consoleErrors).toEqual([])
})

test('管理员批量提交使用排课教师且不允许空提交', async ({ page }) => {
  await installSession(page, { userId: 'UA', username: 'admin', name: '系统管理员', roleId: '1', relatedId: null })
  const consoleErrors = []
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(message.text()) })
  let batchBody = null
  let saved = false

  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    if (path.endsWith('/semester/list')) return reply(route, ok([{ semesterId: 'SEM1', name: '2026-2027学年第一学期', status: 'current' }]))
    if (path.endsWith('/course/list')) return reply(route, ok([{ courseId: 'C1', name: '市场管理' }]))
    if (path.endsWith('/schedule/list')) {
      expect(url.searchParams.get('semesterId')).toBe('SEM1')
      return reply(route, ok([{ scheduleId: 'SC1', courseId: 'C1', teacherId: 'T015', semesterId: 'SEM1', classId: 'CL1' }]))
    }
    if (path.endsWith('/grade/roster')) {
      expect(url.searchParams.has('teacherId')).toBe(false)
      const base = [
        { studentId: 'S009', studentName: '诸葛亮', courseId: 'C1', courseName: '市场管理' },
        { studentId: 'S010', studentName: '李白', courseId: 'C1', courseName: '市场管理' }
      ]
      return reply(route, ok(base.map((item, index) => ({
        ...item, gradeId: saved ? `G${index + 1}` : undefined, semesterId: 'SEM1', teacherId: 'T015',
        usualScore: saved && index === 0 ? 90 : null, examScore: saved && index === 0 ? 94 : null,
        totalScore: saved && index === 0 ? 92.8 : null, status: saved && index === 0 ? 'submitted' : 'draft'
      }))))
    }
    if (path.endsWith('/grade/batch')) {
      expect(route.request().method()).toBe('POST')
      batchBody = route.request().postDataJSON()
      saved = true
      return reply(route, ok(null))
    }
    return reply(route, ok([]))
  })

  await page.goto('/home/grade/input')
  await selectGradeFilters(page)
  await page.getByRole('button', { name: '批量提交' }).click()
  await expect(page.getByText('没有可提交的成绩记录')).toBeVisible()
  expect(batchBody).toBeNull()

  const firstRow = page.locator('.el-table__body tr').filter({ hasText: 'S009' })
  await expect(firstRow).toHaveCount(1)
  const firstRowInputs = firstRow.getByRole('spinbutton')
  await expect(firstRowInputs).toHaveCount(2)
  await firstRowInputs.nth(0).fill('90')
  await firstRowInputs.nth(1).fill('94')
  await page.getByRole('button', { name: '批量提交' }).click()
  await expect(page.getByText('已提交（待审核）')).toBeVisible()
  expect(batchBody).toEqual([{
    studentId: 'S009', courseId: 'C1', semesterId: 'SEM1', teacherId: 'T015', usualScore: 90, examScore: 94
  }])
  expect(consoleErrors).toEqual([])
})
