import axios from 'axios'
import type { AxiosError, AxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'

import { mockAdapter } from '@/mock'
import type { ApiResult } from '@/types/api'

/** 为 true 时所有接口走前端内存 mock，由 .env.development 里的 VITE_USE_MOCK 控制 */
export const useMock = import.meta.env.VITE_USE_MOCK === 'true'

/**
 * 接口返回的错误。
 * fieldErrors 的键是字段名：表单页捕获这个错误后，把提示显示在对应的表单项下面。
 */
export class ApiError extends Error {
  code: number
  status: number
  fieldErrors: Record<string, string>

  constructor(text: string, code: number, status: number, fieldErrors: Record<string, string>) {
    super(text)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const http = axios.create({ baseURL: '/api', timeout: 15000 })

if (useMock) {
  http.defaults.adapter = mockAdapter
}

function readFieldErrors(data: unknown): Record<string, string> {
  if (data && typeof data === 'object' && 'fieldErrors' in data) {
    const fieldErrors = (data as { fieldErrors: unknown }).fieldErrors
    if (fieldErrors && typeof fieldErrors === 'object') {
      return fieldErrors as Record<string, string>
    }
  }
  return {}
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResult<unknown> | undefined>) => {
    const status = error.response?.status ?? 0
    const body = error.response?.data
    const hasBody = body !== null && typeof body === 'object'
    const text = hasBody
      ? body.message
      : status === 0
        ? '连不上后端，请确认后端已经启动'
        : `请求失败（HTTP ${status}），请确认后端已经启动`
    const fieldErrors = hasBody ? readFieldErrors(body.data) : {}
    // 能定位到字段的错误交给表单显示，其它错误弹全局提示
    if (Object.keys(fieldErrors).length === 0) {
      message.error(text)
    }
    return Promise.reject(new ApiError(text, hasBody ? body.code : -1, status, fieldErrors))
  },
)

/** 发请求并取出响应里的 data。失败时抛 ApiError。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResult<T>>(config)
  return response.data.data
}
