import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  dispatch: vi.fn(),
  push: vi.fn(),
  messageError: vi.fn(),
  state: { token: 'jwt-token', user: { roleId: '1' } }
}))

vi.mock('../store', () => ({ default: { state: mocks.state, dispatch: mocks.dispatch } }))
vi.mock('../router', () => ({ default: { currentRoute: { value: { path: '/home/dashboard' } }, push: mocks.push } }))
vi.mock('element-plus', () => ({ ElMessage: { error: mocks.messageError } }))

import request from '../utils/request'

const response = (config, data) => ({ data, status: 200, statusText: 'OK', headers: {}, config })

describe('Axios 请求封装', () => {
  beforeEach(() => {
    mocks.dispatch.mockReset()
    mocks.push.mockReset()
    mocks.messageError.mockReset()
    mocks.state.token = 'jwt-token'
  })

  it('附加 Bearer token 并解包统一响应', async () => {
    request.defaults.adapter = async config => {
      expect(config.headers.Authorization).toBe('Bearer jwt-token')
      return response(config, { code: 200, message: 'ok', data: { value: 1 } })
    }
    await expect(request.get('/test')).resolves.toMatchObject({ data: { value: 1 } })
  })

  it('业务 401 自动退出并返回登录页', async () => {
    request.defaults.adapter = async config => response(config, { code: 401, message: '登录失效', data: null })
    await expect(request.get('/secure')).rejects.toThrow('登录失效')
    expect(mocks.dispatch).toHaveBeenCalledWith('logout')
    expect(mocks.push).toHaveBeenCalledWith('/')
  })

  it('skipErrorMessage 把展示权交给页面', async () => {
    request.defaults.adapter = async config => response(config, { code: 500, message: '失败', data: null })
    await expect(request.get('/quiet', { skipErrorMessage: true })).rejects.toThrow('失败')
    expect(mocks.messageError).not.toHaveBeenCalled()
  })
})

