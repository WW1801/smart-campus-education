import { expect, test } from '@playwright/test'

const ok = (data, message = '操作成功') => ({ code: 200, message, data })
const student = { userId: 'US', username: '2026001001001', name: '李同学', roleId: '5', relatedId: 'S1' }
const admin = { userId: 'UA', username: 'admin', name: '系统管理员', roleId: '1' }

const setSession = async (page, user) => {
  await page.evaluate(sessionUser => {
    localStorage.setItem('token', `token-${sessionUser.roleId}`)
    localStorage.setItem('user', JSON.stringify(sessionUser))
  }, user)
}

const chooseSelect = async (page, dialog, index, label) => {
  await dialog.locator('.el-select').nth(index).click()
  await page.getByRole('option', { name: label, exact: false }).last().click()
}

test('学生提交、重复保护、撤销，管理员审批/拒绝/批量审批并同步考勤', async ({ page }) => {
  test.setTimeout(60000)
  const requests = []
  const attendance = []
  let sequence = 0
  let conflictRequestId = ''
  let conflictResolved = false
  let batchCalls = 0
  let createRequest = null

  await page.addInitScript(value => {
    if (!localStorage.getItem('token')) localStorage.setItem('token', 'token-5')
    if (!localStorage.getItem('user')) localStorage.setItem('user', JSON.stringify(value))
  }, student)

  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const method = route.request().method()
    const path = url.pathname
    let response = ok([])
    if (path.endsWith('/semester/list')) response = ok([{ semesterId: 'SEM1', name: '2026—2027 学年第一学期', startDate: '2026-02-01', endDate: '2026-07-31', status: 'current' }])
    else if (path.endsWith('/course/list')) response = ok([{ courseId: 'C1', code: 'MATH101', name: '高等数学' }])
    else if (path.endsWith('/selection/timetable') && method === 'GET') {
      expect(url.searchParams.get('studentId')).toBe('S1')
      expect(url.searchParams.get('semesterId')).toBe('SEM1')
      response = ok([{ scheduleId: 'SCH1', courseId: 'C1', courseName: '高等数学', semesterId: 'SEM1', mode: 'class_based' }])
    }
    else if (path.endsWith('/leave-requests/my') && method === 'GET') {
      response = ok({ records: requests.filter(item => item.studentId === 'S1'), total: requests.length, current: 1, size: 10 })
    } else if (path.endsWith('/leave-requests/page') && method === 'GET') {
      const status = url.searchParams.get('status')
      const rows = status ? requests.filter(item => item.status === status) : requests
      response = ok({ records: rows, total: rows.length, current: 1, size: 10 })
    } else if (path.endsWith('/leave-requests') && method === 'POST') {
      const body = route.request().postDataJSON()
      createRequest = { method, path, body }
      const duplicate = requests.find(item => item.status !== 'cancelled' && item.courseId === body.courseId
        && item.startDate === body.startDate && item.endDate === body.endDate && item.reason === body.reason)
      if (duplicate) response = { code: 409, message: '该请假申请已提交，请勿重复操作', data: null }
      else {
        const item = { requestId: `LR${++sequence}`, studentId: 'S1', studentNo: '2026001001001', studentName: '李同学',
          courseId: body.courseId, courseName: '高等数学', semesterId: body.semesterId, semesterName: '2026—2027 学年第一学期',
          startDate: body.startDate, endDate: body.endDate, leaveType: body.leaveType, reason: body.reason,
          status: 'pending', attendanceSyncStatus: 'pending', submittedAt: '2026-07-30T10:00:00' }
        requests.push(item)
        response = ok(item, '请假申请已提交')
      }
    } else if (/\/leave-requests\/[^/]+\/cancel$/.test(path)) {
      const item = requests.find(row => path.includes(row.requestId))
      item.status = 'cancelled'; item.attendanceSyncStatus = 'not_required'
      response = ok(item)
    } else if (/\/leave-requests\/[^/]+\/approve$/.test(path)) {
      const item = requests.find(row => path.includes(row.requestId))
      if (item.requestId === conflictRequestId && !conflictResolved) {
        item.attendanceSyncStatus = 'failed'
        response = { code: 409, message: '审批未完成，存在考勤同步冲突', data: {
          requestId: item.requestId, approved: false, attendanceSyncStatus: 'failed', failureDetails: [{
            requestId: item.requestId, date: item.startDate, field: 'attendance',
            reason: '该日期已有非请假考勤：absent', suggestion: '先核对已有考勤事实，系统不会静默覆盖。'
          }]
        } }
      } else {
        item.status = 'approved'; item.attendanceSyncStatus = 'synced'
        attendance.push({ attendanceId: `A-${item.requestId}`, studentId: item.studentId, studentNo: item.studentNo,
          studentName: item.studentName, courseId: item.courseId, courseName: item.courseName, semesterId: item.semesterId,
          date: item.startDate, status: 'leave', recordSource: 'leave_request', sourceRequestId: item.requestId })
        response = ok({ requestId: item.requestId, approved: true, synchronizedCount: 1, attendanceSyncStatus: 'synced', failureDetails: [] })
      }
    } else if (/\/leave-requests\/[^/]+\/reject$/.test(path)) {
      const item = requests.find(row => path.includes(row.requestId)); item.status = 'rejected'; item.attendanceSyncStatus = 'not_required'
      response = ok(item)
    } else if (path.endsWith('/leave-requests/batch-approve')) {
      const ids = route.request().postDataJSON().requestIds
      batchCalls += 1
      if (batchCalls === 1 && ids.length > 1) {
        const successful = requests.find(row => row.requestId === ids[0]); successful.status = 'approved'; successful.attendanceSyncStatus = 'synced'
        const failed = requests.find(row => row.requestId === ids[1]); failed.attendanceSyncStatus = 'failed'
        response = ok({ successCount: 1, failedCount: 1, results: [
          { requestId: ids[0], approved: true, attendanceSyncStatus: 'synced', failureDetails: [] },
          { requestId: ids[1], approved: false, attendanceSyncStatus: 'failed', failureDetails: [{
            requestId: ids[1], date: failed.startDate, field: 'attendance', reason: '已有人工锁定考勤', suggestion: '解除锁定后重试该申请。'
          }] }
        ] })
      } else {
        ids.forEach(id => { const item = requests.find(row => row.requestId === id); item.status = 'approved'; item.attendanceSyncStatus = 'synced' })
        response = ok({ successCount: ids.length, failedCount: 0, results: ids.map(id => ({ requestId: id, approved: true, attendanceSyncStatus: 'synced', failureDetails: [] })) })
      }
    } else if (path.endsWith('/attendance/list')) {
      response = ok({ records: attendance, total: attendance.length, current: 1, size: 10 })
    } else if (path.endsWith('/selection/available') || path.endsWith('/selection/my-courses') || path.endsWith('/selection/my-schedule')) response = ok([])

    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) })
  })

  const submitLeave = async () => {
    await page.getByRole('button', { name: '提交请假' }).click()
    const dialog = page.getByRole('dialog', { name: '提交请假申请' })
    await chooseSelect(page, dialog, 0, '2026—2027 学年第一学期')
    await chooseSelect(page, dialog, 1, '高等数学')
    const dateInputs = dialog.locator('.el-date-editor input')
    await dateInputs.nth(0).fill('2026-03-10'); await dateInputs.nth(0).press('Tab')
    await dateInputs.nth(1).fill('2026-03-10'); await dateInputs.nth(1).press('Tab')
    await chooseSelect(page, dialog, 2, '病假')
    await dialog.getByPlaceholder('请说明请假原因').fill('发热就医')
    await dialog.getByRole('button', { name: '提交申请' }).click()
  }

  await page.goto('/home/leave/my')
  await submitLeave()
  await expect(page.getByText('请假申请已提交', { exact: true })).toBeVisible()
  expect(createRequest).toEqual({
    method: 'POST', path: '/api/leave-requests', body: {
      courseId: 'C1', semesterId: 'SEM1', startDate: '2026-03-10', endDate: '2026-03-10',
      leaveType: 'sick', reason: '发热就医'
    }
  })
  await submitLeave()
  const duplicateDialog = page.getByRole('dialog', { name: '提交请假申请' })
  await expect(duplicateDialog.getByText('该请假申请已提交，请勿重复操作')).toBeVisible()
  await expect(duplicateDialog.getByPlaceholder('请说明请假原因')).toHaveValue('发热就医')
  await duplicateDialog.getByRole('button', { name: '取消' }).click()

  await page.getByRole('button', { name: '撤销' }).click()
  await page.getByRole('dialog', { name: '撤销请假' }).getByRole('button', { name: 'OK' }).click()
  await expect(page.getByText('请假申请已撤销', { exact: true })).toBeVisible()

  await submitLeave()
  await expect(page.getByText('请假申请已提交', { exact: true })).toBeVisible()
  const approvedId = requests.find(item => item.status === 'pending').requestId
  requests.push({ ...requests.find(item => item.requestId === approvedId), requestId: `LR${++sequence}`, startDate: '2026-03-12', endDate: '2026-03-12', reason: '家庭事务' })

  await setSession(page, admin)
  await page.goto('/home/leave/approval')
  const approveRow = page.locator('.el-table__body tr').filter({ hasText: '2026-03-10' })
  await approveRow.getByRole('button', { name: '通过' }).click()
  await page.getByRole('dialog', { name: '通过请假申请' }).getByRole('button', { name: '确认通过' }).click()
  await expect(page.getByText('审批通过，考勤已同步')).toBeVisible()
  expect(attendance).toContainEqual(expect.objectContaining({ sourceRequestId: approvedId, status: 'leave', recordSource: 'leave_request' }))

  await page.goto('/home/attendance/record')
  const syncedAttendanceRow = page.locator('.el-table__body tr').filter({ hasText: '高等数学' })
  await expect(syncedAttendanceRow).toContainText('请假')
  await page.goto('/home/leave/approval')

  conflictRequestId = `LR${++sequence}`
  requests.push({ ...requests[1], requestId: conflictRequestId, status: 'pending', attendanceSyncStatus: 'pending', startDate: '2026-03-13', endDate: '2026-03-13' })
  await page.reload()
  const conflictRow = page.locator('.el-table__body tr').filter({ hasText: '2026-03-13' })
  await conflictRow.getByRole('button', { name: '通过' }).click()
  const conflictDialog = page.getByRole('dialog', { name: '通过请假申请' })
  await conflictDialog.getByRole('button', { name: '确认通过' }).click()
  await expect(conflictDialog.getByText('该日期已有非请假考勤：absent')).toBeVisible()
  await expect(conflictDialog.getByText('先核对已有考勤事实，系统不会静默覆盖。')).toBeVisible()
  conflictResolved = true
  await conflictDialog.getByRole('button', { name: '确认通过' }).click()
  await expect(page.getByText('审批通过，考勤已同步')).toBeVisible()

  const rejectRow = page.locator('.el-table__body tr').filter({ hasText: '2026-03-12' })
  await rejectRow.getByRole('button', { name: '拒绝' }).click()
  await page.getByRole('dialog', { name: '拒绝请假申请' }).getByRole('button', { name: '确认拒绝' }).click()
  await expect(page.getByText('申请已拒绝')).toBeVisible()

  requests.push({ ...requests[1], requestId: `LR${++sequence}`, status: 'pending', attendanceSyncStatus: 'pending', startDate: '2026-03-14' })
  requests.push({ ...requests[1], requestId: `LR${++sequence}`, status: 'pending', attendanceSyncStatus: 'pending', startDate: '2026-03-15' })
  await page.reload()
  await page.locator('.el-table__body .el-checkbox').nth(0).click()
  await page.locator('.el-table__body .el-checkbox').nth(1).click()
  await page.getByRole('button', { name: /批量审批（2）/ }).click()
  const batchDialog = page.getByRole('dialog', { name: '批量通过请假申请' })
  await batchDialog.getByRole('button', { name: '确认通过' }).click()
  await expect(page.getByText('批量审批部分完成：成功 1 条，失败 1 条')).toBeVisible()
  await expect(batchDialog.getByText('已有人工锁定考勤')).toBeVisible()
  await expect(batchDialog.getByText('解除锁定后重试该申请。')).toBeVisible()
  await batchDialog.getByRole('button', { name: '确认通过' }).click()
  await expect(page.getByText('批量审批完成，考勤已同步')).toBeVisible()

  await setSession(page, student)
  await page.goto('/home/leave/approval')
  await expect(page).toHaveURL(/\/home\/selection/)
  await setSession(page, admin)
  await page.goto('/home/leave/my')
  await expect(page).toHaveURL(/\/home\/dashboard/)
})

test('学生请假页面在窄屏下保持可操作', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.addInitScript(value => {
    localStorage.setItem('token', 'token-5')
    localStorage.setItem('user', JSON.stringify(value))
  }, student)
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname
    let response = ok([])
    if (path.endsWith('/semester/list')) response = ok([{ semesterId: 'SEM1', name: '2026—2027 学年第一学期', startDate: '2026-02-01', endDate: '2026-07-31', status: 'current' }])
    else if (path.endsWith('/course/list')) response = ok([{ courseId: 'C1', code: 'MATH101', name: '高等数学' }])
    else if (path.endsWith('/selection/timetable')) response = ok([{ scheduleId: 'SCH1', courseId: 'C1', courseName: '高等数学', semesterId: 'SEM1', mode: 'class_based' }])
    else if (path.endsWith('/leave-requests/my')) response = ok({ records: [], total: 0, current: 1, size: 10 })
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) })
  })

  await page.goto('/home/leave/my')
  await expect(page.getByRole('heading', { name: '我的请假' })).toBeVisible()
  await expect(page.getByRole('button', { name: '提交请假' })).toBeVisible()
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth)
  expect(overflow).toBe(false)
})

test('旧学期不能提交超出学期范围的请假日期', async ({ page }) => {
  let createCalls = 0
  await page.addInitScript(value => {
    localStorage.setItem('token', 'token-5')
    localStorage.setItem('user', JSON.stringify(value))
  }, student)
  await page.route('**/api/**', async route => {
    const url = new URL(route.request().url())
    const path = url.pathname
    const method = route.request().method()
    let response = ok([])
    if (path.endsWith('/semester/list')) response = ok([{ semesterId: 'SEM-OLD', name: '2023—2024 学年第二学期', startDate: '2024-02-20', endDate: '2024-06-30', status: 'current' }])
    else if (path.endsWith('/course/list')) response = ok([
      { courseId: 'C1', code: 'CS101', name: '数据结构' },
      { courseId: 'C2', code: 'CO009', name: '高等数学' }
    ])
    else if (path.endsWith('/selection/timetable')) response = ok([{ scheduleId: 'SCH1', courseId: 'C1', courseName: '数据结构', semesterId: 'SEM-OLD', mode: 'class_based' }])
    else if (path.endsWith('/leave-requests/my')) response = ok({ records: [], total: 0, current: 1, size: 10 })
    else if (path.endsWith('/leave-requests') && method === 'POST') createCalls += 1
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) })
  })

  await page.goto('/home/leave/my')
  await page.getByRole('button', { name: '提交请假' }).click()
  const dialog = page.getByRole('dialog', { name: '提交请假申请' })
  await expect(dialog.getByText('系统没有覆盖今天的学期；如需申请当前日期，请联系教务管理员更新学期与排课。')).toBeVisible()
  await chooseSelect(page, dialog, 0, '2023—2024 学年第二学期')
  await chooseSelect(page, dialog, 1, '数据结构')
  await dialog.locator('.el-date-editor input').nth(0).fill('2026-08-01')
  await dialog.locator('.el-date-editor input').nth(1).fill('2026-08-02')
  await chooseSelect(page, dialog, 2, '病假')
  await dialog.getByPlaceholder('请说明请假原因').fill('身体不适')
  await dialog.getByRole('button', { name: '提交申请' }).click()
  await expect(dialog.getByText('请选择请假日期')).toBeVisible()
  expect(createCalls).toBe(0)
})
