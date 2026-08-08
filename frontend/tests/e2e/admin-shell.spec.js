import { expect, test } from '@playwright/test'

const result = data => ({ code: 200, message: 'success', data })
const runtimeErrors = new WeakMap()

test.beforeEach(async ({ page }) => {
  const errors = []
  runtimeErrors.set(page, errors)
  page.on('console', message => {
    if (message.type() === 'error') errors.push(message.text())
  })
  page.on('pageerror', error => errors.push(error.message))
  await page.addInitScript(() => {
    localStorage.setItem('token', 'e2e-token')
    localStorage.setItem('user', JSON.stringify({ userId: 'U1', username: 'admin', name: '系统管理员', roleId: '1', roleName: '管理员' }))
  })
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    let data = []
    if (url.pathname.endsWith('/semester/list')) data = [{ semesterId: 'S1', name: '2026—2027 学年第一学期', status: 'current' }]
    if (url.pathname.endsWith('/agent/analysis')) data = { dataOverview: { studentCount: 10, activeStudentCount: 9, gradePassRate: 90, attendanceRecordCount: 20, abnormalAttendanceCount: 1, graduationAuditCount: 2, approvedGraduationCount: 2, graduationApprovalRate: 100 }, insights: ['一条考勤记录需要核查'] }
    if (url.pathname.endsWith('/agent/warnings/students')) data = { records: [], total: 0 }
    if (url.pathname.endsWith('/grade/statistics')) data = {
      total: 120,
      average: 82.5,
      passRate: '94%',
      excellentRate: '31%',
      distribution: { fail: 7, pass: 18, medium: 32, good: 38, excellent: 25 },
      courseAvgList: [{ courseId: 'C1', name: '高等数学', avg: 84 }]
    }
    if (url.pathname.endsWith('/account/password')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 400, message: '当前密码不正确', data: null }) })
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(result(data)) })
  })
})

test.afterEach(async ({ page }) => {
  expect(runtimeErrors.get(page) || []).toEqual([])
})

test('管理员外壳、AI 路由与移动抽屉可用', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.goto('/home/dashboard')
  await expect(page.getByRole('heading', { name: '数据看板' })).toBeVisible()
  const desktopNav = page.locator('#campus-navigation')
  await expect.poll(() => desktopNav.getByRole('menuitem').count()).toBeGreaterThanOrEqual(20)
  await expect(desktopNav.getByRole('menuitem', { name: /数据看板/ })).toBeVisible()
  await expect(desktopNav.getByRole('menuitem', { name: /AI 数据分析/ })).toBeVisible()
  await expect(desktopNav.getByRole('menuitem', { name: /学业预警/ })).toBeVisible()
  await page.getByRole('button', { name: 'AI 分析' }).click()
  await expect(page).toHaveURL(/\/home\/agent\/analysis/)

  await page.goto('/home/dashboard')
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('button', { name: '打开导航' }).click()
  await expect(page.getByRole('menuitem', { name: '数据看板' })).toBeFocused()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('button', { name: '打开导航' })).toBeFocused()
})

test('未知路由显示 404 恢复页面', async ({ page }) => {
  await page.goto('/not-a-campus-route')
  await expect(page.getByRole('heading', { name: '没有找到这个教务页面' })).toBeVisible()
})

test('320 至 1440 宽度无横向溢出，移动端账号入口保持可用', async ({ page }) => {
  for (const width of [1440, 1024, 768, 390, 320]) {
    await page.setViewportSize({ width, height: 900 })
    await page.goto('/home/dashboard')
    await expect(page.getByRole('heading', { name: '数据看板' })).toBeVisible()
    const hasHorizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth)
    expect(hasHorizontalOverflow, `${width}px 不应出现页面级横向滚动`).toBeFalsy()
  }

  await expect(page.getByRole('button', { name: '打开账号菜单' })).toBeVisible()
  await page.getByRole('button', { name: '打开账号菜单' }).click()
  await expect(page.getByRole('menuitem', { name: /admin · 系统管理员/ })).toBeVisible()
  await expect(page.getByRole('menuitem', { name: '系统管理员', exact: true })).toBeVisible()
})

test('成绩图表在手机端纵向排列并保留图表文本替代', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto('/home/grade/statistics')
  await expect(page.getByText('参考人数').locator('..').getByText('120')).toBeVisible()
  await expect(page.getByRole('img', { name: /图表类型是柱状图.*0-59/ })).toBeVisible()
  await expect(page.getByRole('img', { name: /图表类型是柱状图.*高等数学/ })).toBeVisible()
  await expect(page.getByRole('table', { name: '成绩分布数据' })).toContainText('90-100')
  await expect(page.getByRole('table', { name: '各课程平均分' })).toContainText('高等数学')

  const columns = page.locator('.chart-grid > .el-col')
  await expect(columns).toHaveCount(2)
  const firstBox = await columns.nth(0).boundingBox()
  const secondBox = await columns.nth(1).boundingBox()
  expect(secondBox.y).toBeGreaterThan(firstBox.y + firstBox.height - 2)
  expect(Math.abs(firstBox.width - secondBox.width)).toBeLessThan(2)
})

test('修改密码失败保留输入，退出登录清除认证并回登录页', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 })
  await page.goto('/home/dashboard')
  await page.getByRole('button', { name: '修改密码' }).click()
  const passwordDialog = page.getByRole('dialog', { name: '修改密码' })
  await expect(passwordDialog).toBeVisible()
  await passwordDialog.getByPlaceholder('请输入当前密码').fill('old-pass')
  await passwordDialog.getByPlaceholder('请输入 8 至 64 位，包含字母和数字').fill('new-pass-123')
  await passwordDialog.getByPlaceholder('请再次输入新密码').fill('new-pass-123')
  await passwordDialog.getByRole('button', { name: '确认修改' }).click()
  await expect(page.getByText('当前密码不正确')).toBeVisible()
  await expect(passwordDialog.getByPlaceholder('请输入当前密码')).toHaveValue('old-pass')
  await expect(passwordDialog.getByPlaceholder('请输入 8 至 64 位，包含字母和数字')).toHaveValue('new-pass-123')
  await passwordDialog.getByRole('button', { name: '取消' }).click()

  await page.getByRole('button', { name: '退出登录' }).click()
  const logoutDialog = page.locator('.logout-confirm')
  await expect(logoutDialog).toBeVisible()
  await expect(logoutDialog).toHaveCSS('background-color', 'rgb(255, 255, 255)')
  await expect.poll(async () => {
    const dialogBox = await logoutDialog.boundingBox()
    const viewport = page.viewportSize()
    return Math.abs(dialogBox.x + dialogBox.width / 2 - viewport.width / 2)
  }).toBeLessThan(2)
  await expect.poll(async () => {
    const dialogBox = await logoutDialog.boundingBox()
    const viewport = page.viewportSize()
    return Math.abs(dialogBox.y + dialogBox.height / 2 - viewport.height / 2)
  }).toBeLessThan(2)

  await logoutDialog.getByRole('button', { name: '继续使用' }).click()
  await page.setViewportSize({ width: 390, height: 844 })
  await page.getByRole('button', { name: '打开账号菜单' }).click()
  await page.getByRole('menuitem', { name: '退出登录' }).click()
  await expect(logoutDialog).toBeVisible()
  await expect.poll(async () => {
    const dialogBox = await logoutDialog.boundingBox()
    const viewport = page.viewportSize()
    return Math.max(
      Math.abs(dialogBox.x + dialogBox.width / 2 - viewport.width / 2),
      Math.abs(dialogBox.y + dialogBox.height / 2 - viewport.height / 2)
    )
  }).toBeLessThan(2)
  const mobileDialogBox = await logoutDialog.boundingBox()
  expect(mobileDialogBox.width).toBeLessThanOrEqual(358)
  await logoutDialog.getByRole('button', { name: '退出登录' }).click()
  await expect(page).toHaveURL(/\/$/)
  const auth = await page.evaluate(() => ({ token: localStorage.getItem('token'), user: localStorage.getItem('user') }))
  expect(auth).toEqual({ token: null, user: null })
})
