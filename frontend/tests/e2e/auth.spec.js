import { expect, test } from '@playwright/test'

const ok = data => ({ code: 200, message: '操作成功', data })

test.beforeEach(async ({ page }) => {
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    if (url.pathname.endsWith('/login')) {
      const body = route.request().postDataJSON()
      const response = body.password === 'correct-password'
        ? ok({ token: 'e2e-token', user: { userId: 'U1', username: body.username, name: '系统管理员', roleId: '1' } })
        : { code: 401, message: '用户名或密码错误', data: null }
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) })
      return
    }
    const data = url.pathname.endsWith('/semester/list') ? []
      : url.pathname.endsWith('/agent/analysis') ? { dataOverview: {}, insights: [] }
        : url.pathname.endsWith('/agent/warnings/students') ? { records: [], total: 0 }
          : []
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(data)) })
  })
})

test('登录成功后进入角色首页', async ({ page }) => {
  await page.goto('/')
  await page.getByPlaceholder('请输入学号、工号或管理员用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('correct-password')
  await page.getByRole('button', { name: '登 录' }).click()
  await expect(page).toHaveURL(/\/home\/dashboard/)
  await expect(page.getByRole('heading', { name: '数据看板' })).toBeVisible()
})

test('错误密码显示统一错误且保留账号', async ({ page }) => {
  await page.goto('/')
  await page.getByPlaceholder('请输入学号、工号或管理员用户名').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('wrong-password')
  await page.getByRole('button', { name: '登 录' }).click()
  await expect(page.getByText('用户名或密码错误')).toBeVisible()
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByPlaceholder('请输入学号、工号或管理员用户名')).toHaveValue('admin')
})

test('未登录访问受限路由被送回登录页', async ({ page }) => {
  await page.goto('/home/leave/approval')
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('heading', { name: '欢迎登录' })).toBeVisible()
})
