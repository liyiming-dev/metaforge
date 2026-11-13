// 接口的请求和响应类型，和 docs/contract/api.md 一一对应。契约改了，这里要跟着改。

import type { ModelDef, ModelInput, ModuleDef, SortItem } from './meta'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface Health {
  status: string
  database: string
  time: string
}

export interface MenuNode {
  key: string
  displayName: string
  icon?: string
  /** 叶子节点才有：对应的模型名 */
  model?: string
  /** 叶子节点才有：前端路由地址，固定是 /app/ 加模型名 */
  path?: string
  children?: MenuNode[]
}

export interface MetaBundle {
  modules: ModuleDef[]
  models: ModelDef[]
}

export interface MetaBundleInput {
  modules?: ModuleDef[]
  models?: ModelInput[]
}

export interface ImportResult {
  modules: number
  models: number
}

/** 一条业务数据。键是字段名，值的格式见契约里的字段类型表 */
export type DataRecord = Record<string, unknown>

/** manyToOne 字段在响应里的值 */
export interface RefValue {
  id: number
  label: string
}

export type FilterOp = 'eq' | 'ne' | 'like' | 'gt' | 'ge' | 'lt' | 'le' | 'in'

export interface Filter {
  field: string
  op: FilterOp
  value: unknown
}

export interface SearchRequest {
  page?: number
  size?: number
  filters?: Filter[]
  sort?: SortItem[]
}

export interface PageResult {
  records: DataRecord[]
  total: number
  page: number
  size: number
}

export interface OptionItem {
  value: number
  label: string
}

export interface OptionsQuery {
  keyword?: string
  limit?: number
  displayField?: string
}

export interface ServiceCall {
  /** scope 为 record 的服务必填 */
  id?: number
  params?: Record<string, unknown>
}
