import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import AxiosMockAdapter from 'axios-mock-adapter'
import { request, registerMessageHandler } from '../request'
import type { R } from '@sca/types'

// 模拟 @sca/utils 的 token 操作
vi.mock('@sca/utils', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  clearToken: vi.fn()
}))

// 模拟 crypto.randomUUID（Node 测试环境无 crypto.randomUUID）
Object.defineProperty(globalThis, 'crypto', {
  value: { randomUUID: vi.fn(() => 'test-trace-id') }
})

// 模拟 window.location
const originalLocation = window.location
delete (window as any).location
window.location = { href: '', pathname: '/dashboard' } as any

describe('request.ts axios 拦截器', () => {
  let mock: AxiosMockAdapter

  beforeEach(() => {
    mock = new AxiosMockAdapter(request.raw())
    window.location.href = ''
  })

  afterEach(() => {
    mock.restore()
  })

  it('HTTP 200 + bizCode 00000 应成功返回 data', async () => {
    const response: R<string> = {
      code: 200,
      bizCode: '00000',
      message: '操作成功',
      data: 'payload',
      timestamp: new Date().toISOString(),
      traceId: 'trace-1',
      path: '/api/test'
    }
    mock.onGet('/api/test').reply(200, response)

    const data = await request.get<string>('/api/test')
    expect(data).toBe('payload')
  })

  it('HTTP 200 + bizCode 非 00000 应业务失败并 reject', async () => {
    const response: R<null> = {
      code: 200,
      bizCode: '01105',
      message: '用户名或密码错误',
      data: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-2',
      path: '/api/auth/login'
    }
    mock.onPost('/api/auth/login').reply(200, response)

    const messages: string[] = []
    registerMessageHandler((content) => messages.push(content))

    await expect(request.post('/api/auth/login', {})).rejects.toMatchObject({
      bizCode: '01105'
    })
    expect(messages).toContain('用户名或密码错误')
  })

  it('HTTP 401 应清空 Token 并跳转登录', async () => {
    const response: R<null> = {
      code: 401,
      bizCode: '01301',
      message: '用户已被禁用',
      data: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-3',
      path: '/api/auth/login'
    }
    mock.onPost('/api/auth/login').reply(401, response)

    await expect(request.post('/api/auth/login', {})).rejects.toBeDefined()
    const { clearToken } = await import('@sca/utils')
    expect(clearToken).toHaveBeenCalled()
    expect(window.location.href).toMatch(/\/login\?redirect=/)
  })

  it('HTTP 403 应显示错误消息并 reject', async () => {
    const response: R<null> = {
      code: 403,
      bizCode: '00302',
      message: '无权访问该数据范围',
      data: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-4',
      path: '/api/system/users'
    }
    mock.onGet('/api/system/users').reply(403, response)

    const messages: string[] = []
    registerMessageHandler((content) => messages.push(content))

    await expect(request.get('/api/system/users')).rejects.toBeDefined()
    expect(messages).toContain('无权访问该数据范围')
  })

  it('HTTP 429 应显示错误消息并 reject', async () => {
    const response: R<null> = {
      code: 429,
      bizCode: '00201',
      message: '请求过于频繁，请稍后重试',
      data: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-5',
      path: '/api/system/users'
    }
    mock.onGet('/api/system/users').reply(429, response)

    const messages: string[] = []
    registerMessageHandler((content) => messages.push(content))

    await expect(request.get('/api/system/users')).rejects.toBeDefined()
    expect(messages).toContain('请求过于频繁，请稍后重试')
  })

  it('HTTP 500 应显示错误消息并 reject', async () => {
    const response: R<null> = {
      code: 500,
      bizCode: '00401',
      message: '系统繁忙，请稍后重试',
      data: null,
      timestamp: new Date().toISOString(),
      traceId: 'trace-6',
      path: '/api/system/users'
    }
    mock.onGet('/api/system/users').reply(500, response)

    const messages: string[] = []
    registerMessageHandler((content) => messages.push(content))

    await expect(request.get('/api/system/users')).rejects.toBeDefined()
    expect(messages).toContain('系统繁忙，请稍后重试')
  })

  it('请求拦截器应自动携带 X-Trace-Id', async () => {
    mock.onGet('/api/test').reply((config) => {
      expect(config.headers?.['X-Trace-Id']).toBe('test-trace-id')
      return [200, {
        code: 200,
        bizCode: '00000',
        message: '操作成功',
        data: null,
        timestamp: new Date().toISOString()
      } as R<null>]
    })

    await request.get('/api/test')
  })
})

// 恢复 window.location
describe('cleanup', () => {
  it('restore location', () => {
    window.location.href = originalLocation.href
  })
})
