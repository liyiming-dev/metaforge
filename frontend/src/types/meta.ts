// 元模型的类型定义，和 docs/contract/meta-model.md 一一对应。契约改了，这里要跟着改。

export type FieldType =
  | 'string'
  | 'text'
  | 'integer'
  | 'decimal'
  | 'boolean'
  | 'date'
  | 'datetime'
  | 'enum'
  | 'manyToOne'
  | 'oneToMany'

export interface EnumOption {
  value: string
  label: string
}

export interface RelationDef {
  /** 关联到哪个模型 */
  model: string
  /** manyToOne 专用：用目标模型的哪个字段当显示文字 */
  displayField?: string
  /** oneToMany 专用：子表模型里指回本模型的 manyToOne 字段名 */
  mappedBy?: string
}

export interface FieldDef {
  name: string
  displayName: string
  type: FieldType
  required?: boolean
  unique?: boolean
  readonly?: boolean
  defaultValue?: unknown
  /** string 专用 */
  length?: number
  /** decimal 专用 */
  precision?: number
  /** decimal 专用 */
  scale?: number
  /** enum 必填 */
  options?: EnumOption[]
  /** manyToOne、oneToMany 必填 */
  relation?: RelationDef
}

export type ServiceTrigger =
  | 'manual'
  | 'beforeCreate'
  | 'afterCreate'
  | 'beforeUpdate'
  | 'afterUpdate'
  | 'beforeDelete'

export interface ServiceDef {
  name: string
  displayName: string
  trigger: ServiceTrigger
  /** manual 专用：record 针对一条记录，model 不针对具体记录。默认 record */
  scope?: 'record' | 'model'
  script: string
  enabled?: boolean
}

export interface SortItem {
  field: string
  order: 'asc' | 'desc'
}

export interface ListViewDef {
  columns: string[]
  searchFields: string[]
  defaultSort?: SortItem
  pageSize?: number
}

export interface FormGroupDef {
  title: string
  /** 每行放几个字段，1 到 4 */
  columns: number
  fields: string[]
}

export interface FormTabDef {
  title: string
  /** 必须是一个 oneToMany 字段 */
  field: string
}

export interface FormViewDef {
  groups: FormGroupDef[]
  tabs: FormTabDef[]
}

export interface ViewsDef {
  list: ListViewDef
  form: FormViewDef
}

export interface ModuleDef {
  name: string
  displayName: string
  icon?: string
  sort?: number
}

/** 后端返回的模型定义：services 和 views 一定存在 */
export interface ModelDef {
  name: string
  displayName: string
  description?: string
  module: string
  showInMenu?: boolean
  sort?: number
  fields: FieldDef[]
  services: ServiceDef[]
  views: ViewsDef
  /** 只读，后端生成 */
  tableName?: string
}

/** 提交给后端的模型定义：services 和 views 可以不传 */
export type ModelInput = Omit<ModelDef, 'services' | 'views' | 'tableName'> & {
  services?: ServiceDef[]
  views?: ViewsDef
}
