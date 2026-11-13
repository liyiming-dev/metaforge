// mock 用的“假后端”：把元模型和业务数据放在浏览器内存里，按 docs/contract/api.md 的规则处理请求。
// 刷新页面后数据会恢复成初始状态。它只是为了让前端在后端没写完时也能开发，
// 校验比真实后端宽松，也不执行脚本服务。联调通过后可以整个删掉 src/mock。

import type {
  DataRecord,
  Filter,
  ImportResult,
  MenuNode,
  MetaBundle,
  MetaBundleInput,
  OptionItem,
  PageResult,
  SearchRequest,
} from '@/types/api'
import type { FieldDef, ModelDef, ModelInput, ModuleDef, SortItem, ViewsDef } from '@/types/meta'

import { mesBundle, mesSeed } from './mes-sample'

export class MockError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, text: string, fieldErrors: Record<string, string> = {}) {
    super(text)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const NAME_PATTERN = /^[a-z][a-z0-9_]{0,29}$/
const SYSTEM_FIELDS = ['id', 'created_at', 'updated_at']

const modules: ModuleDef[] = structuredClone(mesBundle.modules)
const models: ModelDef[] = structuredClone(mesBundle.models)
const tables = new Map<string, DataRecord[]>()
const lastIds = new Map<string, number>()

export function now(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return `${date} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function rowsOf(model: string): DataRecord[] {
  let rows = tables.get(model)
  if (!rows) {
    rows = []
    tables.set(model, rows)
  }
  return rows
}

function nextId(model: string): number {
  const id = (lastIds.get(model) ?? 0) + 1
  lastIds.set(model, id)
  return id
}

for (const [model, seed] of Object.entries(mesSeed)) {
  for (const row of seed) {
    const time = now()
    rowsOf(model).push({ id: nextId(model), ...row, created_at: time, updated_at: time })
  }
}

const bySort = (a: { sort?: number }, b: { sort?: number }) => (a.sort ?? 100) - (b.sort ?? 100)

// ---------- 元模型 ----------

function checkName(name: unknown, what: string): void {
  if (typeof name !== 'string' || !NAME_PATTERN.test(name)) {
    throw new MockError(
      400,
      `${what}“${String(name)}”不合规则：小写字母开头，只含小写字母、数字、下划线，最长 30 位`,
    )
  }
}

export function listModules(): ModuleDef[] {
  return [...modules].sort(bySort)
}

export function saveModule(name: string, input: ModuleDef): ModuleDef {
  checkName(input.name, '模块名')
  if (input.name !== name) {
    throw new MockError(400, '地址里的模块名和请求体里的不一致')
  }
  if (!input.displayName) {
    throw new MockError(400, '模块的显示名不能为空')
  }
  const index = modules.findIndex((m) => m.name === name)
  if (index >= 0) {
    modules[index] = input
  } else {
    modules.push(input)
  }
  return input
}

export function removeModule(name: string): null {
  const index = modules.findIndex((m) => m.name === name)
  if (index < 0) {
    throw new MockError(404, `模块 ${name} 不存在`)
  }
  if (models.some((m) => m.module === name)) {
    throw new MockError(409, '模块下还有模型，不能删除')
  }
  modules.splice(index, 1)
  return null
}

export function listModels(): ModelDef[] {
  return models
}

export function getModel(name: string): ModelDef {
  const model = models.find((m) => m.name === name)
  if (!model) {
    throw new MockError(404, `模型 ${name} 不存在`)
  }
  return model
}

function defaultViews(fields: FieldDef[]): ViewsDef {
  const stored = fields.filter((f) => f.type !== 'oneToMany')
  return {
    list: {
      columns: stored.filter((f) => f.type !== 'text').map((f) => f.name),
      searchFields: fields
        .filter((f) => f.type === 'string' || f.type === 'enum')
        .slice(0, 3)
        .map((f) => f.name),
      defaultSort: { field: 'created_at', order: 'desc' },
      pageSize: 10,
    },
    form: {
      groups: [{ title: '基本信息', columns: 2, fields: stored.map((f) => f.name) }],
      tabs: fields
        .filter((f) => f.type === 'oneToMany')
        .map((f) => ({ title: f.displayName, field: f.name })),
    },
  }
}

/** 校验并补齐默认值。这里的校验比真实后端宽松，例如不检查关联指向的模型是否存在。 */
function normalizeModel(input: ModelInput): ModelDef {
  checkName(input.name, '模型名')
  if (!input.displayName) {
    throw new MockError(400, '模型的显示名不能为空')
  }
  if (!modules.some((m) => m.name === input.module)) {
    throw new MockError(400, `模块 ${String(input.module)} 不存在`)
  }
  if (!Array.isArray(input.fields) || input.fields.length === 0) {
    throw new MockError(400, '模型至少要有一个字段')
  }
  const seen = new Set<string>()
  for (const field of input.fields) {
    checkName(field.name, '字段名')
    if (SYSTEM_FIELDS.includes(field.name)) {
      throw new MockError(400, `${field.name} 是系统字段，不能用作字段名`)
    }
    if (seen.has(field.name)) {
      throw new MockError(400, `字段名 ${field.name} 重复`)
    }
    seen.add(field.name)
    if (!field.displayName) {
      throw new MockError(400, `字段 ${field.name} 的显示名不能为空`)
    }
    if (field.type === 'enum' && !field.options?.length) {
      throw new MockError(400, `枚举字段 ${field.name} 至少要有一个选项`)
    }
    const isRelation = field.type === 'manyToOne' || field.type === 'oneToMany'
    if (isRelation && !field.relation?.model) {
      throw new MockError(400, `关联字段 ${field.name} 没有指定关联的模型`)
    }
  }
  return {
    ...input,
    showInMenu: input.showInMenu ?? true,
    sort: input.sort ?? 100,
    services: input.services ?? [],
    views: input.views ?? defaultViews(input.fields),
    tableName: `md_${input.name}`,
  }
}

export function createModel(input: ModelInput): ModelDef {
  if (models.some((m) => m.name === input.name)) {
    throw new MockError(409, `模型 ${input.name} 已存在`)
  }
  const model = normalizeModel(input)
  models.push(model)
  return model
}

export function updateModel(name: string, input: ModelInput): ModelDef {
  const index = models.findIndex((m) => m.name === name)
  const old = models[index]
  if (!old) {
    throw new MockError(404, `模型 ${name} 不存在`)
  }
  if (input.name !== name) {
    throw new MockError(400, '地址里的模型名和请求体里的不一致')
  }
  for (const field of input.fields ?? []) {
    const before = old.fields.find((f) => f.name === field.name)
    if (before && before.type !== field.type) {
      throw new MockError(400, `不能修改字段 ${field.name} 的类型`)
    }
  }
  const model = normalizeModel(input)
  models[index] = model
  return model
}

export function removeModel(name: string): null {
  const index = models.findIndex((m) => m.name === name)
  if (index < 0) {
    throw new MockError(404, `模型 ${name} 不存在`)
  }
  const referrer = models.find(
    (m) => m.name !== name && m.fields.some((f) => f.relation?.model === name),
  )
  if (referrer) {
    throw new MockError(409, `模型 ${name} 被“${referrer.displayName}”的关联字段引用，不能删除`)
  }
  models.splice(index, 1)
  tables.delete(name)
  return null
}

export function exportBundle(): MetaBundle {
  return { modules: listModules(), models }
}

export function importBundle(bundle: MetaBundleInput): ImportResult {
  const moduleInputs = bundle.modules ?? []
  const modelInputs = bundle.models ?? []
  for (const module of moduleInputs) {
    saveModule(module.name, module)
  }
  for (const input of modelInputs) {
    if (models.some((m) => m.name === input.name)) {
      updateModel(input.name, input)
    } else {
      createModel(input)
    }
  }
  return { modules: moduleInputs.length, models: modelInputs.length }
}

export function buildMenus(): MenuNode[] {
  const menus: MenuNode[] = []
  for (const module of listModules()) {
    const children = models
      .filter((m) => m.module === module.name && m.showInMenu !== false)
      .sort(bySort)
      .map((m): MenuNode => ({
        key: m.name,
        displayName: m.displayName,
        model: m.name,
        path: `/app/${m.name}`,
      }))
    if (children.length > 0) {
      menus.push({ key: module.name, displayName: module.displayName, icon: module.icon, children })
    }
  }
  return menus
}

// ---------- 业务数据 ----------

/** 找一个占数据库列的字段（oneToMany 不占列） */
function columnField(model: ModelDef, name: string): FieldDef | undefined {
  return model.fields.find((f) => f.name === name && f.type !== 'oneToMany')
}

function isColumn(model: ModelDef, name: string): boolean {
  return SYSTEM_FIELDS.includes(name) || columnField(model, name) !== undefined
}

/** manyToOne 的值可能是 id，也可能是 { id, label }，统一取出 id */
function refId(value: unknown): unknown {
  if (value !== null && typeof value === 'object' && 'id' in value) {
    return (value as { id: unknown }).id
  }
  return value
}

function same(a: unknown, b: unknown): boolean {
  return a === b || (a != null && b != null && String(a) === String(b))
}

function compare(a: unknown, b: unknown): number {
  if (a == null || b == null) {
    return a == null ? (b == null ? 0 : -1) : 1
  }
  if (typeof a === 'number' && !Number.isNaN(Number(b))) {
    return a - Number(b)
  }
  return String(a).localeCompare(String(b))
}

function matches(row: DataRecord, filter: Filter): boolean {
  const actual = row[filter.field]
  const expected = refId(filter.value)
  switch (filter.op) {
    case 'eq':
      return same(actual, expected)
    case 'ne':
      return !same(actual, expected)
    case 'like':
      return String(actual ?? '').includes(String(expected ?? ''))
    case 'gt':
      return actual != null && compare(actual, expected) > 0
    case 'ge':
      return actual != null && compare(actual, expected) >= 0
    case 'lt':
      return actual != null && compare(actual, expected) < 0
    case 'le':
      return actual != null && compare(actual, expected) <= 0
    case 'in':
      return Array.isArray(filter.value) && filter.value.some((item) => same(actual, refId(item)))
    default:
      throw new MockError(400, `不支持的比较方式 ${String(filter.op)}`)
  }
}

/** 把内存里的一行变成响应里的记录：manyToOne 的 id 换成 { id, label } */
function toResponse(model: ModelDef, row: DataRecord): DataRecord {
  const record: DataRecord = { ...row }
  for (const field of model.fields) {
    if (field.type !== 'manyToOne' || !field.relation) {
      continue
    }
    const id = row[field.name]
    if (id == null) {
      record[field.name] = null
      continue
    }
    const target = rowsOf(field.relation.model).find((r) => same(r.id, id))
    const label = target?.[field.relation.displayField ?? 'id']
    record[field.name] = { id, label: String(label ?? id) }
  }
  return record
}

function findRow(modelName: string, id: number): DataRecord {
  const row = rowsOf(modelName).find((r) => r.id === id)
  if (!row) {
    throw new MockError(404, `${modelName} 里没有 id 为 ${id} 的记录`)
  }
  return row
}

export function search(modelName: string, body: SearchRequest | null): PageResult {
  const model = getModel(modelName)
  const filters = body?.filters ?? []
  const fallbackSort: SortItem = model.views.list.defaultSort ?? { field: 'created_at', order: 'desc' }
  const sort = body?.sort?.length ? body.sort : [fallbackSort]
  for (const item of [...filters, ...sort]) {
    if (!isColumn(model, item.field)) {
      throw new MockError(400, `模型 ${modelName} 没有字段 ${item.field}`)
    }
  }

  const matched = rowsOf(modelName).filter((row) => filters.every((filter) => matches(row, filter)))
  const descending = sort[0]?.order === 'desc'
  matched.sort((a, b) => {
    for (const item of sort) {
      const diff = compare(a[item.field], b[item.field])
      if (diff !== 0) {
        return item.order === 'desc' ? -diff : diff
      }
    }
    // 排序字段的值相同时按 id 排，保证顺序稳定
    return descending ? compare(b.id, a.id) : compare(a.id, b.id)
  })

  const page = Math.max(1, body?.page ?? 1)
  const size = Math.min(200, Math.max(1, body?.size ?? model.views.list.pageSize ?? 10))
  const records = matched.slice((page - 1) * size, page * size).map((row) => toResponse(model, row))
  return { records, total: matched.length, page, size }
}

export function getRecord(modelName: string, id: number): DataRecord {
  return toResponse(getModel(modelName), findRow(modelName, id))
}

/** 把请求里的值写进目标行：忽略系统字段，拒绝模型里没有的字段 */
function assign(model: ModelDef, target: DataRecord, input: DataRecord | null): void {
  for (const [key, value] of Object.entries(input ?? {})) {
    if (SYSTEM_FIELDS.includes(key)) {
      continue
    }
    const field = columnField(model, key)
    if (!field) {
      throw new MockError(400, `模型 ${model.name} 没有字段 ${key}`)
    }
    const plain = field.type === 'manyToOne' ? refId(value) : value
    target[key] = plain === '' || plain === undefined ? null : plain
  }
}

function validate(model: ModelDef, row: DataRecord, selfId: number | null): void {
  const invalid: Record<string, string> = {}
  const duplicated: Record<string, string> = {}
  for (const field of model.fields) {
    if (field.type === 'oneToMany') {
      continue
    }
    const value = row[field.name]
    if (value == null) {
      if (field.required) {
        invalid[field.name] = `${field.displayName}不能为空`
      }
      continue
    }
    if (field.type === 'enum' && !field.options?.some((option) => option.value === value)) {
      invalid[field.name] = `${field.displayName}的值不在选项里`
    }
    if (
      field.type === 'manyToOne' &&
      field.relation &&
      !rowsOf(field.relation.model).some((r) => same(r.id, value))
    ) {
      invalid[field.name] = `${field.displayName}指向的记录不存在`
    }
    if (
      field.unique &&
      rowsOf(model.name).some((r) => r.id !== selfId && same(r[field.name], value))
    ) {
      duplicated[field.name] = `${field.displayName}已存在`
    }
  }
  const firstInvalid = Object.values(invalid)[0]
  if (firstInvalid) {
    throw new MockError(400, firstInvalid, invalid)
  }
  const firstDuplicated = Object.values(duplicated)[0]
  if (firstDuplicated) {
    throw new MockError(409, firstDuplicated, duplicated)
  }
}

export function createRecord(modelName: string, input: DataRecord | null): DataRecord {
  const model = getModel(modelName)
  const row: DataRecord = {}
  for (const field of model.fields) {
    if (field.type !== 'oneToMany') {
      row[field.name] = field.defaultValue ?? null
    }
  }
  assign(model, row, input)

  const id = nextId(modelName)
  // mock 不执行脚本。这里粗略模仿“保存前自动生成编码”的钩子，方便在页面上看到效果。
  const hasBeforeCreate = model.services.some((s) => s.trigger === 'beforeCreate')
  if (hasBeforeCreate && columnField(model, 'code') && row.code == null) {
    row.code = `MOCK-${String(id).padStart(4, '0')}`
  }

  validate(model, row, null)
  const time = now()
  const saved: DataRecord = { id, ...row, created_at: time, updated_at: time }
  rowsOf(modelName).push(saved)
  return toResponse(model, saved)
}

export function updateRecord(modelName: string, id: number, input: DataRecord | null): DataRecord {
  const model = getModel(modelName)
  const row = findRow(modelName, id)
  const draft: DataRecord = { ...row }
  assign(model, draft, input)
  validate(model, draft, id)
  Object.assign(row, draft, { updated_at: now() })
  return toResponse(model, row)
}

export function removeRecord(modelName: string, id: number): null {
  getModel(modelName)
  const rows = rowsOf(modelName)
  const index = rows.findIndex((r) => r.id === id)
  if (index < 0) {
    throw new MockError(404, `${modelName} 里没有 id 为 ${id} 的记录`)
  }
  for (const other of models) {
    for (const field of other.fields) {
      const pointsHere = field.type === 'manyToOne' && field.relation?.model === modelName
      if (pointsHere && rowsOf(other.name).some((r) => same(r[field.name], id))) {
        throw new MockError(409, `这条记录被“${other.displayName}”引用，不能删除`)
      }
    }
  }
  rows.splice(index, 1)
  return null
}

export function listOptions(
  modelName: string,
  keyword: string,
  limit: number,
  displayField: string,
): OptionItem[] {
  const model = getModel(modelName)
  const labelField = displayField || model.fields.find((f) => f.type === 'string')?.name || 'id'
  return rowsOf(modelName)
    .filter((row) => !keyword || String(row[labelField] ?? '').includes(keyword))
    .slice(0, Math.min(100, Math.max(1, limit || 20)))
    .map((row) => ({ value: Number(row.id), label: String(row[labelField] ?? row.id) }))
}

export function callService(modelName: string, serviceName: string): string {
  const model = getModel(modelName)
  const service = model.services.find((s) => s.name === serviceName && s.trigger === 'manual')
  if (!service) {
    throw new MockError(404, `模型 ${modelName} 没有可以手动调用的服务 ${serviceName}`)
  }
  return `mock 不执行脚本。连上真实后端后，这里会是“${service.displayName}”的返回值`
}
