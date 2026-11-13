// axios 的 mock 适配器：VITE_USE_MOCK=true 时，request.ts 把所有请求交给这里，不发到网络上。
// 路由表和 docs/contract/api.md 一一对应，具体逻辑在 store.ts。

import { AxiosError } from 'axios'
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

import type { DataRecord, MetaBundleInput, SearchRequest } from '@/types/api'
import type { ModelInput, ModuleDef } from '@/types/meta'

import * as store from './store'

interface MockRequest {
  /** 地址里用括号捕获的部分，按顺序排列 */
  params: string[]
  body: unknown
  query: URLSearchParams
}

type Handler = (request: MockRequest) => unknown
type Route = [method: string, pattern: RegExp, handler: Handler]

const param = (request: MockRequest, index: number) => request.params[index] ?? ''
const idParam = (request: MockRequest, index: number) => Number(param(request, index))

const routes: Route[] = [
  ['get', /^\/health$/, () => ({ status: 'MOCK', database: '前端内存 mock', time: store.now() })],

  ['get', /^\/meta\/menus$/, () => store.buildMenus()],
  ['get', /^\/meta\/modules$/, () => store.listModules()],
  ['put', /^\/meta\/modules\/([^/]+)$/, (r) => store.saveModule(param(r, 0), r.body as ModuleDef)],
  ['delete', /^\/meta\/modules\/([^/]+)$/, (r) => store.removeModule(param(r, 0))],
  ['get', /^\/meta\/models$/, () => store.listModels()],
  ['get', /^\/meta\/models\/([^/]+)$/, (r) => store.getModel(param(r, 0))],
  ['post', /^\/meta\/models$/, (r) => store.createModel(r.body as ModelInput)],
  ['put', /^\/meta\/models\/([^/]+)$/, (r) => store.updateModel(param(r, 0), r.body as ModelInput)],
  ['delete', /^\/meta\/models\/([^/]+)$/, (r) => store.removeModel(param(r, 0))],
  ['get', /^\/meta\/export$/, () => store.exportBundle()],
  ['post', /^\/meta\/import$/, (r) => store.importBundle(r.body as MetaBundleInput)],

  ['post', /^\/data\/([^/]+)\/search$/, (r) => store.search(param(r, 0), r.body as SearchRequest | null)],
  [
    'get',
    /^\/data\/([^/]+)\/options$/,
    (r) =>
      store.listOptions(
        param(r, 0),
        r.query.get('keyword') ?? '',
        Number(r.query.get('limit') ?? 20),
        r.query.get('displayField') ?? '',
      ),
  ],
  ['get', /^\/data\/([^/]+)\/(\d+)$/, (r) => store.getRecord(param(r, 0), idParam(r, 1))],
  ['post', /^\/data\/([^/]+)$/, (r) => store.createRecord(param(r, 0), r.body as DataRecord | null)],
  [
    'put',
    /^\/data\/([^/]+)\/(\d+)$/,
    (r) => store.updateRecord(param(r, 0), idParam(r, 1), r.body as DataRecord | null),
  ],
  ['delete', /^\/data\/([^/]+)\/(\d+)$/, (r) => store.removeRecord(param(r, 0), idParam(r, 1))],

  ['post', /^\/svc\/([^/]+)\/([^/]+)$/, (r) => store.callService(param(r, 0), param(r, 1))],

  [
    'post',
    /^\/ai\/generate$/,
    () => {
      throw new store.MockError(400, 'mock 模式下没有 AI 助手，请连接真实后端')
    },
  ],
]

function respond(config: InternalAxiosRequestConfig, status: number, data: unknown): AxiosResponse {
  return { data, status, statusText: status === 200 ? 'OK' : 'Error', headers: {}, config }
}

export const mockAdapter: AxiosAdapter = async (config) => {
  // 模拟一点网络延迟，这样加载状态能被看见
  await new Promise((resolve) => setTimeout(resolve, 120))

  const method = (config.method ?? 'get').toLowerCase()
  const url = new URL(config.url ?? '/', 'http://mock.local')
  const query = new URLSearchParams(url.search)
  for (const [key, value] of Object.entries((config.params ?? {}) as Record<string, unknown>)) {
    if (value !== undefined && value !== null && value !== '') {
      query.set(key, String(value))
    }
  }
  const body: unknown =
    typeof config.data === 'string' && config.data !== '' ? JSON.parse(config.data) : config.data

  try {
    for (const [routeMethod, pattern, handler] of routes) {
      const match = routeMethod === method ? pattern.exec(url.pathname) : null
      if (!match) {
        continue
      }
      const data = handler({ params: match.slice(1).map(decodeURIComponent), body, query })
      // 复制一份再返回，避免页面改动响应对象时把 mock 里的数据也改了
      return respond(config, 200, { code: 0, message: 'ok', data: structuredClone(data) })
    }
    throw new store.MockError(404, `mock 里没有这个接口：${method.toUpperCase()} ${url.pathname}`)
  } catch (error) {
    if (!(error instanceof store.MockError)) {
      throw error
    }
    const hasFieldErrors = Object.keys(error.fieldErrors).length > 0
    const response = respond(config, error.status, {
      code: error.status * 100,
      message: error.message,
      data: hasFieldErrors ? { fieldErrors: error.fieldErrors } : null,
    })
    throw new AxiosError(error.message, AxiosError.ERR_BAD_REQUEST, config, null, response)
  }
}
