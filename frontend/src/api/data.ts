import type {
  DataRecord,
  MetaBundle,
  OptionItem,
  OptionsQuery,
  PageResult,
  SearchRequest,
  ServiceCall,
} from '@/types/api'

import { request } from './request'

/** 数据接口，见 docs/contract/api.md 的“数据接口”。model 是模型名 */
export const dataApi = {
  search: (model: string, body: SearchRequest = {}) =>
    request<PageResult>({ url: `/data/${model}/search`, method: 'post', data: body }),
  get: (model: string, id: number) => request<DataRecord>({ url: `/data/${model}/${id}` }),
  create: (model: string, record: DataRecord) =>
    request<DataRecord>({ url: `/data/${model}`, method: 'post', data: record }),
  update: (model: string, id: number, record: DataRecord) =>
    request<DataRecord>({ url: `/data/${model}/${id}`, method: 'put', data: record }),
  remove: (model: string, id: number) =>
    request<null>({ url: `/data/${model}/${id}`, method: 'delete' }),
  options: (model: string, query: OptionsQuery = {}) =>
    request<OptionItem[]>({ url: `/data/${model}/options`, params: query }),
}

/** 服务接口：调用一个 trigger 为 manual 的脚本服务，返回脚本的返回值 */
export function callService(model: string, service: string, body: ServiceCall = {}) {
  return request<unknown>({ url: `/svc/${model}/${service}`, method: 'post', data: body })
}

/** AI 接口（加分项）：只生成元模型包，不保存 */
export function generateByAi(prompt: string) {
  return request<MetaBundle>({ url: '/ai/generate', method: 'post', data: { prompt } })
}
