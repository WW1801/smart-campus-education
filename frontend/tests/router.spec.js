import { describe, expect, it } from 'vitest'
import router, { getDefaultHomePath, resolveRouteAccess } from '../router'

describe('路由权限决策', () => {
  it.each([
    ['1', '/home/dashboard'],
    ['2', '/home/student/info'],
    ['3', '/home/student/info'],
    ['4', '/home/grade/query'],
    ['5', '/home/selection']
  ])('角色 %s 默认进入 %s', (roleId, path) => {
    expect(getDefaultHomePath(roleId)).toBe(path)
  })

  it('未登录访问受保护路由时返回登录页', () => {
    expect(resolveRouteAccess(
      { path: '/home/dashboard', requiresAuth: true, requiredRoles: ['1'] },
      { token: '', user: null }
    )).toMatchObject({ redirect: '/', reason: 'invalid-auth' })
  })

  it('无权限角色返回自己的默认首页', () => {
    expect(resolveRouteAccess(
      { path: '/home/system/user', requiresAuth: true, requiredRoles: ['1'] },
      { token: 'token', user: { roleId: '5' } }
    )).toMatchObject({ redirect: '/home/selection', reason: 'forbidden' })
  })

  it('保留 AI 正式地址、兼容旧地址并注册 404', () => {
    const routes = router.getRoutes()
    expect(routes.some(route => route.path === '/home/agent/analysis')).toBe(true)
    expect(routes.find(route => route.path === '/home/analysis')?.redirect).toBe('/home/agent/analysis')
    expect(routes.some(route => route.name === 'NotFound')).toBe(true)
  })
})

