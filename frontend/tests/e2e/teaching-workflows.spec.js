import { expect, test } from '@playwright/test'

const ok = data => ({ code: 200, message: '操作成功', data })
const admin = { userId: 'UA', username: 'admin', name: '系统管理员', roleId: '1' }

test.beforeEach(async ({ page }) => {
  await page.addInitScript(user => {
    localStorage.setItem('token', 'e2e-admin-token')
    localStorage.setItem('user', JSON.stringify(user))
  }, admin)
})

const choose = async (page, container, index, label) => {
  await container.locator('.el-select').nth(index).click()
  await page.getByRole('option', { name: label, exact: false }).last().click()
}

test('自动排课冲突保留弹窗与真实建议，修改条件后重试并刷新列表', async ({ page }) => {
  let arrangeCalls = 0
  const schedules = []
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    let data = []
    if (path.endsWith('/semester/list')) data = [{ semesterId: 'SEM1', name: '2026—2027 学年第一学期', status: 'current' }]
    else if (path.endsWith('/course/list')) data = [{ courseId: 'C1', name: '高等数学' }]
    else if (path.endsWith('/teacher/list')) data = [{ teacherId: 'T1', name: '张老师' }]
    else if (path.endsWith('/classroom/list')) data = [{ classroomId: 'R1', name: '博学楼 101', capacity: 80 }]
    else if (path.endsWith('/class/list')) data = [{ classId: 'CL1', name: '计科 2026-1 班' }]
    else if (path.endsWith('/schedule/page')) data = { records: schedules, total: schedules.length }
    else if (path.endsWith('/schedule-basic/auto-arrange')) {
      arrangeCalls++
      if (arrangeCalls === 1) {
        data = {
        arrangedCount: 0, failedCount: 1,
        failureDetails: [{ index: 0, priority: 'P0', type: 'teacher', reason: '张老师在周一第1-2节已有课程',
          suggestion: '将候选时间调整为第3-4节后直接重试', conflictCourseName: '大学物理', conflictTeacherName: '张老师',
          conflictDayOfWeek: 1, conflictStartPeriod: 1, conflictEndPeriod: 2 }]
        }
      } else {
        schedules.push({ scheduleId: 'SC1', semesterId: 'SEM1', courseId: 'C1', teacherId: 'T1', classId: 'CL1',
          classroomId: 'R1', mode: 'class_based', dayOfWeek: 1, startPeriod: 3, endPeriod: 4 })
        data = { arrangedCount: 1, failedCount: 0, failureDetails: [] }
      }
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(data)) })
  })

  await page.goto('/home/schedule/arrange')
  await page.getByRole('button', { name: '自动排课' }).click()
  const dialog = page.getByRole('dialog', { name: '自动排课' })
  await choose(page, dialog, 1, '高等数学')
  await choose(page, dialog, 2, '张老师')
  await choose(page, dialog, 4, '计科 2026-1 班')
  await choose(page, dialog, 5, '博学楼 101')
  await dialog.getByRole('button', { name: '开始排课' }).click()

  await expect(dialog).toBeVisible()
  await expect(dialog.getByText('张老师在周一第1-2节已有课程')).toBeVisible()
  await expect(dialog.getByText('建议：将候选时间调整为第3-4节后直接重试')).toBeVisible()
  await expect(dialog.getByText('冲突课程：大学物理')).toBeVisible()

  await dialog.getByRole('spinbutton', { name: '开始节次' }).fill('3')
  await dialog.getByRole('spinbutton', { name: '结束节次' }).fill('4')
  await dialog.getByRole('button', { name: '开始排课' }).click()
  await expect(dialog).toBeHidden()
  const scheduleRow = page.locator('.el-table__body tr').filter({ hasText: '周1 第3-4节' })
  await expect(scheduleRow.getByText('高等数学')).toBeVisible()
  expect(arrangeCalls).toBe(2)
})

test('批量考勤部分失败显示学生、原因和建议，修正后可再次保存', async ({ page }) => {
  let saveCalls = 0
  const roster = [
    { studentId: 'S1', studentNo: '2026001001001', studentName: '李同学', courseId: 'C1', courseName: '高等数学', semesterId: 'SEM1', date: '2026-03-10', status: '' },
    { studentId: 'S2', studentNo: '2026001001002', studentName: '王同学', courseId: 'C1', courseName: '高等数学', semesterId: 'SEM1', date: '2026-03-10', status: '' }
  ]
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    let data = []
    if (path.endsWith('/semester/list')) data = [{ semesterId: 'SEM1', name: '2026—2027 学年第一学期', status: 'current' }]
    else if (path.endsWith('/course/list')) data = [{ courseId: 'C1', name: '高等数学' }]
    else if (path.endsWith('/class/list')) data = []
    else if (path.endsWith('/schedule/list')) data = [{ scheduleId: 'SC1', courseId: 'C1', semesterId: 'SEM1' }]
    else if (path.endsWith('/student/page')) data = { records: [], total: 0 }
    else if (path.endsWith('/attendance/list')) data = { records: [], total: 0 }
    else if (path.endsWith('/attendance/roster')) data = roster
    else if (path.endsWith('/attendance/batch-save')) {
      saveCalls++
      const records = route.request().postDataJSON().records
      if (saveCalls === 1) {
        roster[0].status = records.find(item => item.studentId === 'S1').status
        data = { successCount: 1, failedCount: 1, successDetails: [{ studentId: 'S1' }],
          failureDetails: [{ index: 1, studentId: 'S2', studentNo: '2026001001002', studentName: '王同学', field: 'status',
            reason: '该状态与当前考勤锁定规则冲突', suggestion: '改为出勤并再次批量保存。' }] }
      } else {
        records.forEach(record => { roster.find(item => item.studentId === record.studentId).status = record.status })
        data = { successCount: 2, failedCount: 0, successDetails: records, failureDetails: [] }
      }
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(ok(data)) })
  })

  await page.goto('/home/attendance/record')
  await page.getByPlaceholder('选择日期').fill('2026-03-10')
  await page.getByRole('button', { name: '查询' }).click()
  await expect(page.getByText('李同学')).toBeVisible()

  const firstRow = page.locator('.el-table__body tr').filter({ hasText: '李同学' }).first()
  const secondRow = page.locator('.el-table__body tr').filter({ hasText: '王同学' }).first()
  await firstRow.locator('.el-select').click(); await page.getByRole('option', { name: '出勤', exact: true }).last().click()
  await secondRow.locator('.el-select').click(); await page.getByRole('option', { name: '缺勤', exact: true }).last().click()
  await page.getByRole('button', { name: '批量保存考勤' }).click()

  await expect(page.getByText('以下记录未保存，修改状态后可直接再次批量保存')).toBeVisible()
  await expect(page.getByText('王同学').last()).toBeVisible()
  await expect(page.getByText('该状态与当前考勤锁定规则冲突')).toBeVisible()
  await expect(page.getByText('改为出勤并再次批量保存。')).toBeVisible()

  const failedRow = page.locator('.el-table__body tr').filter({ hasText: '王同学' }).first()
  await failedRow.locator('.el-select').click(); await page.getByRole('option', { name: '出勤', exact: true }).last().click()
  await page.getByRole('button', { name: '批量保存考勤' }).click()
  await expect(page.getByText('已保存 2 条考勤记录')).toBeVisible()
  await expect(page.getByText('以下记录未保存，修改状态后可直接再次批量保存')).toBeHidden()
  expect(saveCalls).toBe(2)
})
