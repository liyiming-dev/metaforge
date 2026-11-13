import type { ApiResult, Health } from '@/types/api'

/**
 * 请求真实后端的健康检查。故意不走 request.ts，这样 mock 模式下也能看出后端有没有启动。
 */
export async function fetchBackendHealth(): Promise<Health> {
  const response = await fetch('/api/health')
  const body = (await response.json().catch(() => null)) as ApiResult<Health> | null
  if (!response.ok || !body || body.code !== 0) {
    throw new Error(body?.message ?? `后端没有响应（HTTP ${response.status}）`)
  }
  return body.data
}
