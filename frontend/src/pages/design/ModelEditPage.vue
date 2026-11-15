<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { Component } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'

import { metaApi } from '@/api/meta'
import { useMenuStore } from '@/stores/menu'
import { useModelStore } from '@/stores/model'
import { fieldComponents } from '@/components/fields'
import type { FieldDef, ModelDef, ModelInput, ModuleDef, RelationDef, ServiceDef, SortItem, ViewsDef } from '@/types/meta'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const modelStore = useModelStore()

const NAME_PATTERN = /^[a-z][a-z0-9_]{0,29}$/
const SYSTEM_FIELDS = ['id', 'created_at', 'updated_at']

const FIELD_TYPE_OPTIONS = [
  { value: 'string', label: '单行文本' },
  { value: 'text', label: '多行文本' },
  { value: 'integer', label: '整数' },
  { value: 'decimal', label: '小数' },
  { value: 'boolean', label: '是否' },
  { value: 'date', label: '日期' },
  { value: 'datetime', label: '日期时间' },
  { value: 'enum', label: '枚举' },
  { value: 'manyToOne', label: '多对一' },
  { value: 'oneToMany', label: '一对多' },
]

const TRIGGER_OPTIONS = [
  { value: 'manual', label: '手动调用' },
  { value: 'beforeCreate', label: '新建前' },
  { value: 'afterCreate', label: '新建后' },
  { value: 'beforeUpdate', label: '修改前' },
  { value: 'afterUpdate', label: '修改后' },
  { value: 'beforeDelete', label: '删除前' },
]

const SCOPE_OPTIONS = [
  { value: 'record', label: '针对一条记录' },
  { value: 'model', label: '不针对具体记录' },
]

const modelName = computed(() => (typeof route.params.name === 'string' ? route.params.name : ''))
const isEdit = computed(() => modelName.value !== '')

const modules = ref<ModuleDef[]>([])
const draft = ref<ModelInput>({
  name: '',
  displayName: '',
  description: '',
  module: '',
  showInMenu: true,
  sort: 100,
  fields: [],
  services: [],
  views: undefined,
})
const savedFields = ref<Set<string>>(new Set())
const activeTab = ref('basic')
const saving = ref(false)

function typeLabel(type: string): string {
  return FIELD_TYPE_OPTIONS.find((o) => o.value === type)?.label ?? type
}

function isSavedField(name: string): boolean {
  return savedFields.value.has(name)
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

function initCreate(): void {
  draft.value = {
    name: '',
    displayName: '',
    description: '',
    module: modules.value[0]?.name ?? '',
    showInMenu: true,
    sort: 100,
    fields: [],
    services: [],
    views: undefined,
  }
  savedFields.value = new Set()
}

async function initEdit(name: string): Promise<void> {
  const model = await metaApi.model(name)
  draft.value = JSON.parse(JSON.stringify(model)) as ModelInput
  savedFields.value = new Set(model.fields.map((f) => f.name))
}

onMounted(async () => {
  try {
    modules.value = await metaApi.modules()
  } catch {
    modules.value = []
  }
  loadAllModels()
  if (isEdit.value) {
    await initEdit(modelName.value)
  } else {
    initCreate()
  }
})

// ---------- 字段 ----------

function addField(): void {
  draft.value.fields.push({
    name: '',
    displayName: '',
    type: 'string',
    required: false,
    unique: false,
    readonly: false,
  })
}

function removeField(index: number): void {
  const name = draft.value.fields[index]?.name ?? ''
  draft.value.fields.splice(index, 1)
  if (draft.value.views && name) {
    draft.value.views.list.columns = draft.value.views.list.columns.filter((c) => c !== name)
    draft.value.views.list.searchFields = draft.value.views.list.searchFields.filter((c) => c !== name)
    for (const group of draft.value.views.form.groups) {
      group.fields = group.fields.filter((f) => f !== name)
    }
    draft.value.views.form.tabs = draft.value.views.form.tabs.filter((t) => t.field !== name)
  }
}

function moveField(index: number, delta: number): void {
  const target = index + delta
  if (target < 0 || target >= draft.value.fields.length) return
  const arr = draft.value.fields
  const [item] = arr.splice(index, 1)
  arr.splice(target, 0, item)
}

// 字段名填写后，若使用自定义视图，自动把新字段加进视图
function onFieldNameChanged(field: FieldDef): void {
  const views = draft.value.views
  if (!views || !field.name) return
  if (field.type !== 'text' && field.type !== 'oneToMany' && !views.list.columns.includes(field.name)) {
    views.list.columns.push(field.name)
  }
  const firstGroup = views.form.groups[0]
  if (firstGroup && field.type !== 'oneToMany' && !firstGroup.fields.includes(field.name)) {
    firstGroup.fields.push(field.name)
  }
}

function onFieldTypeChanged(field: FieldDef): void {
  // 切换类型时清掉不属于新类型的配置项
  delete field.length
  delete field.precision
  delete field.scale
  delete field.options
  delete field.relation
  if (field.type === 'string') field.length = 255
  if (field.type === 'decimal') {
    field.precision = 18
    field.scale = 4
  }
  if (field.type === 'enum') field.options = []
  if (field.type === 'manyToOne') field.relation = { model: '', displayField: '' }
  if (field.type === 'oneToMany') field.relation = { model: '', mappedBy: '' }
}

// ---------- 字段配置弹窗 ----------

const configOpen = ref(false)
const configIndex = ref<number | null>(null)

const configField = computed(() =>
  configIndex.value != null ? draft.value.fields[configIndex.value] : null,
)

function relationOf(field: FieldDef): RelationDef {
  return field.relation as RelationDef
}

function ensureDefaultSort(): SortItem {
  const views = draft.value.views
  if (views && !views.list.defaultSort) {
    views.list.defaultSort = { field: 'created_at', order: 'desc' }
  }
  return (views?.list.defaultSort ?? { field: 'created_at', order: 'desc' }) as SortItem
}

function setDefaultSortOrder(v: string): void {
  ensureDefaultSort().order = v === 'asc' ? 'asc' : 'desc'
}

function removeEnumOption(field: FieldDef, index: number): void {
  field.options?.splice(index, 1)
}

function onConfigDefaultChange(v: unknown): void {
  const field = configField.value
  if (field) field.defaultValue = v
}

function openFieldConfig(index: number): void {
  configIndex.value = index
  const field = draft.value.fields[index]
  if (field.type === 'enum' && !field.options) field.options = []
  if ((field.type === 'manyToOne' || field.type === 'oneToMany') && !field.relation) {
    field.relation = field.type === 'manyToOne'
      ? { model: '', displayField: '' }
      : { model: '', mappedBy: '' }
  }
  configOpen.value = true
}

function addOption(): void {
  const field = configField.value
  if (field?.options) field.options.push({ value: '', label: '' })
}

function showDefaultValue(field: FieldDef): boolean {
  return field.type !== 'manyToOne' && field.type !== 'oneToMany'
}

// 所有模型定义：关联字段的"关联模型"下拉、显示字段、回指字段都用它
const allModelDefs = ref<ModelDef[]>([])

const allModels = computed(() => allModelDefs.value.map((m) => ({ name: m.name, displayName: m.displayName })))

async function loadAllModels(): Promise<void> {
  try {
    allModelDefs.value = await metaApi.models()
  } catch {
    allModelDefs.value = []
  }
}

// 显示字段（manyToOne）：目标模型里 string 类型的字段
function displayFieldsOf(targetModel: string): { name: string; displayName: string }[] {
  const model = allModelDefs.value.find((m) => m.name === targetModel)
  return model?.fields.filter((f) => f.type === 'string').map((f) => ({ name: f.name, displayName: f.displayName })) ?? []
}

// 回指字段（oneToMany）：目标模型里 manyToOne 类型的字段
function mappedByFieldsOf(targetModel: string): { name: string; displayName: string }[] {
  const model = allModelDefs.value.find((m) => m.name === targetModel)
  return model?.fields.filter((f) => f.type === 'manyToOne').map((f) => ({ name: f.name, displayName: f.displayName })) ?? []
}

// ---------- 视图 ----------

const useDefaultView = computed({
  get: () => draft.value.views === undefined,
  set: (v: boolean) => {
    draft.value.views = v ? undefined : defaultViews(draft.value.fields)
  },
})

const listColumnOptions = computed(() => {
  const fieldOptions = draft.value.fields
    .filter((f) => f.type !== 'oneToMany')
    .map((f) => ({ value: f.name, label: f.displayName || f.name }))
  const systemOptions = SYSTEM_FIELDS.map((name) => ({ value: name, label: name }))
  return [...fieldOptions, ...systemOptions]
})

const searchFieldOptions = computed(() =>
  draft.value.fields
    .filter((f) => f.type !== 'oneToMany' && f.type !== 'text')
    .map((f) => ({ value: f.name, label: f.displayName || f.name })),
)

const oneToManyFields = computed(() => draft.value.fields.filter((f) => f.type === 'oneToMany'))

function addGroup(): void {
  draft.value.views?.form.groups.push({ title: '', columns: 2, fields: [] })
}

function removeGroup(index: number): void {
  draft.value.views?.form.groups.splice(index, 1)
}

function toggleTab(fieldName: string, checked: boolean): void {
  const views = draft.value.views
  if (!views) return
  if (checked) {
    if (!views.form.tabs.find((t) => t.field === fieldName)) {
      const field = draft.value.fields.find((f) => f.name === fieldName)
      views.form.tabs.push({ title: field?.displayName ?? fieldName, field: fieldName })
    }
  } else {
    views.form.tabs = views.form.tabs.filter((t) => t.field !== fieldName)
  }
}

function setTabTitle(fieldName: string, title: string): void {
  const tab = draft.value.views?.form.tabs.find((t) => t.field === fieldName)
  if (tab) tab.title = title
}

// ---------- 服务 ----------

function addService(): void {
  const service: ServiceDef = {
    name: '',
    displayName: '',
    trigger: 'manual',
    scope: 'record',
    script: '',
    enabled: true,
  }
  if (!draft.value.services) draft.value.services = []
  draft.value.services.push(service)
}

function removeService(index: number): void {
  draft.value.services?.splice(index, 1)
}

// ---------- 保存 ----------

function validate(): { ok: boolean; tab: string; message?: string } {
  if (!NAME_PATTERN.test(draft.value.name)) {
    return { ok: false, tab: 'basic', message: '模型标识要小写字母开头，只含小写字母、数字、下划线，最长 30 位' }
  }
  if (!draft.value.displayName.trim()) {
    return { ok: false, tab: 'basic', message: '显示名不能为空' }
  }
  if (draft.value.fields.length === 0) {
    return { ok: false, tab: 'fields', message: '至少要有一个字段' }
  }
  const seen = new Set<string>()
  for (const field of draft.value.fields) {
    if (!NAME_PATTERN.test(field.name)) {
      return { ok: false, tab: 'fields', message: `字段标识「${field.name || '（空）'}」不合规则` }
    }
    if (SYSTEM_FIELDS.includes(field.name)) {
      return { ok: false, tab: 'fields', message: `${field.name} 是系统字段，不能用作字段名` }
    }
    if (seen.has(field.name)) {
      return { ok: false, tab: 'fields', message: `字段标识 ${field.name} 重复` }
    }
    seen.add(field.name)
    if (!field.displayName.trim()) {
      return { ok: false, tab: 'fields', message: `字段 ${field.name} 的显示名不能为空` }
    }
    if (field.type === 'enum' && !(field.options && field.options.length > 0)) {
      return { ok: false, tab: 'fields', message: `枚举字段 ${field.name} 至少要有一个选项` }
    }
    if ((field.type === 'manyToOne' || field.type === 'oneToMany') && !field.relation?.model) {
      return { ok: false, tab: 'fields', message: `关联字段 ${field.name} 没有选择关联模型` }
    }
  }
  return { ok: true, tab: '' }
}

async function onSave(): Promise<void> {
  const result = validate()
  if (!result.ok) {
    if (result.message) message.error(result.message)
    activeTab.value = result.tab
    return
  }
  saving.value = true
  try {
    if (isEdit.value) {
      await metaApi.updateModel(modelName.value, draft.value)
    } else {
      await metaApi.createModel(draft.value)
    }
    message.success('已保存')
    modelStore.invalidate(draft.value.name)
    await menuStore.reload()
    if (!isEdit.value) {
      router.push(`/design/models/${draft.value.name}`)
    } else {
      // 编辑后刷新草稿（重新取定义，更新 savedFields）
      await initEdit(draft.value.name)
    }
  } catch {
    // 失败提示由 request.ts 弹
  } finally {
    saving.value = false
  }
}

async function copyJson(): Promise<void> {
  const text = JSON.stringify(draft.value, null, 2)
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制')
  } catch {
    message.info('复制失败，请手动复制')
  }
}
</script>

<template>
  <a-card :bordered="false">
    <template #title>
      <a-space>
        <a-button size="small" @click="router.push('/design/models')">返回</a-button>
        <span>{{ isEdit ? `编辑模型：${draft.displayName || modelName}` : '新建模型' }}</span>
      </a-space>
    </template>
    <template #extra>
      <a-button type="primary" :loading="saving" @click="onSave">保存</a-button>
    </template>

    <a-tabs v-model:active-key="activeTab">
      <!-- 标签页 1：基本信息 -->
      <a-tab-pane key="basic" tab="基本信息">
        <a-form layout="vertical" class="tab-form">
          <a-form-item label="标识" required>
            <a-input
              v-model:value="draft.name"
              :disabled="isEdit"
              placeholder="小写字母开头，只含小写字母、数字、下划线，最长 30 位"
            />
          </a-form-item>
          <a-form-item label="显示名" required>
            <a-input v-model:value="draft.displayName" />
          </a-form-item>
          <a-form-item label="说明">
            <a-textarea v-model:value="draft.description" :rows="2" />
          </a-form-item>
          <a-form-item label="所属模块">
            <a-select v-model:value="draft.module">
              <a-select-option v-for="m in modules" :key="m.name" :value="m.name">
                {{ m.displayName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="显示在菜单">
            <a-switch v-model:checked="draft.showInMenu" />
          </a-form-item>
          <a-form-item label="排序">
            <a-input-number v-model:value="draft.sort" style="width: 200px" />
          </a-form-item>
        </a-form>
      </a-tab-pane>

      <!-- 标签页 2：字段 -->
      <a-tab-pane key="fields" tab="字段">
        <div class="field-header">
          <span class="field-col name">标识</span>
          <span class="field-col name">显示名</span>
          <span class="field-col type">类型</span>
          <span class="field-col flag">必填</span>
          <span class="field-col flag">唯一</span>
          <span class="field-col flag">只读</span>
          <span class="field-col ops">操作</span>
        </div>
        <div v-for="(field, index) in draft.fields" :key="index" class="field-row">
          <a-input
            v-model:value="field.name"
            class="field-col name"
            :disabled="isSavedField(field.name)"
            placeholder="标识"
            @change="onFieldNameChanged(field)"
          />
          <a-input v-model:value="field.displayName" class="field-col name" placeholder="显示名" />
          <a-select
            v-model:value="field.type"
            class="field-col type"
            :disabled="isSavedField(field.name)"
            @change="onFieldTypeChanged(field)"
          >
            <a-select-option v-for="opt in FIELD_TYPE_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
          <a-checkbox v-model:checked="field.required" class="field-col flag" :disabled="field.type === 'oneToMany'" />
          <a-checkbox v-model:checked="field.unique" class="field-col flag" :disabled="field.type === 'oneToMany'" />
          <a-checkbox v-model:checked="field.readonly" class="field-col flag" :disabled="field.type === 'oneToMany'" />
          <span class="field-col ops">
            <a-button size="small" @click="openFieldConfig(index)">配置</a-button>
            <a-button size="small" :disabled="index === 0" @click="moveField(index, -1)">上移</a-button>
            <a-button size="small" :disabled="index === draft.fields.length - 1" @click="moveField(index, 1)">
              下移
            </a-button>
            <a-button size="small" danger @click="removeField(index)">删除</a-button>
          </span>
        </div>
        <a-button class="add-btn" @click="addField">添加字段</a-button>
      </a-tab-pane>

      <!-- 标签页 3：视图 -->
      <a-tab-pane key="views" tab="视图">
        <a-form layout="vertical" class="tab-form">
          <a-form-item label="使用默认视图">
            <a-switch v-model:checked="useDefaultView" />
            <div v-if="useDefaultView" class="hint">保存后由系统按默认规则生成视图</div>
          </a-form-item>
          <template v-if="!useDefaultView && draft.views">
            <a-form-item label="列表显示的列">
              <a-select v-model:value="draft.views.list.columns" mode="multiple" :options="listColumnOptions" />
            </a-form-item>
            <a-form-item label="搜索字段">
              <a-select v-model:value="draft.views.list.searchFields" mode="multiple" :options="searchFieldOptions" />
            </a-form-item>
            <a-form-item label="默认排序">
              <a-space>
                <a-select
                  :value="ensureDefaultSort().field"
                  style="width: 200px"
                  :options="listColumnOptions"
                  @update:value="(v: string) => (ensureDefaultSort().field = v)"
                />
                <a-select :value="ensureDefaultSort().order" style="width: 100px" @update:value="setDefaultSortOrder">
                  <a-select-option value="asc">升序</a-select-option>
                  <a-select-option value="desc">降序</a-select-option>
                </a-select>
              </a-space>
            </a-form-item>
            <a-form-item label="每页条数">
              <a-input-number v-model:value="draft.views.list.pageSize" :min="1" style="width: 200px" />
            </a-form-item>
            <a-form-item label="表单分组">
              <div v-for="(group, gi) in draft.views.form.groups" :key="gi" class="group-row">
                <a-input v-model:value="group.title" placeholder="组标题" style="width: 160px" />
                <a-select v-model:value="group.columns" style="width: 120px">
                  <a-select-option :value="1">每行 1 个</a-select-option>
                  <a-select-option :value="2">每行 2 个</a-select-option>
                  <a-select-option :value="3">每行 3 个</a-select-option>
                  <a-select-option :value="4">每行 4 个</a-select-option>
                </a-select>
                <a-select v-model:value="group.fields" mode="multiple" :options="searchFieldOptions" style="min-width: 260px" />
                <a-button size="small" danger @click="removeGroup(gi)">删除</a-button>
              </div>
              <a-button size="small" @click="addGroup">添加分组</a-button>
            </a-form-item>
            <a-form-item label="子表标签页">
              <div v-for="field in oneToManyFields" :key="field.name" class="group-row">
                <a-checkbox
                  :checked="!!draft.views.form.tabs.find((t) => t.field === field.name)"
                  @update:checked="(v: boolean) => toggleTab(field.name, v)"
                >
                  {{ field.displayName || field.name }}
                </a-checkbox>
                <a-input
                  :value="draft.views.form.tabs.find((t) => t.field === field.name)?.title ?? ''"
                  placeholder="标题"
                  style="width: 160px"
                  @update:value="(v: string) => setTabTitle(field.name, v)"
                />
              </div>
            </a-form-item>
          </template>
        </a-form>
      </a-tab-pane>

      <!-- 标签页 4：服务 -->
      <a-tab-pane key="services" tab="服务">
        <div v-for="(service, index) in draft.services" :key="index" class="service-row">
          <a-input v-model:value="service.name" placeholder="标识" style="width: 160px" />
          <a-input v-model:value="service.displayName" placeholder="显示名" style="width: 160px" />
          <a-select v-model:value="service.trigger" style="width: 120px">
            <a-select-option v-for="opt in TRIGGER_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
          <a-select v-if="service.trigger === 'manual'" v-model:value="service.scope" style="width: 140px">
            <a-select-option v-for="opt in SCOPE_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </a-select-option>
          </a-select>
          <a-switch v-model:checked="service.enabled" />
          <a-button size="small" danger @click="removeService(index)">删除</a-button>
          <a-textarea v-model:value="service.script" :rows="8" placeholder="脚本" class="script" />
        </div>
        <a-button class="add-btn" @click="addService">添加服务</a-button>
      </a-tab-pane>

      <!-- 标签页 5：JSON -->
      <a-tab-pane key="json" tab="JSON">
        <div class="json-toolbar">
          <a-button size="small" @click="copyJson">复制</a-button>
        </div>
        <pre class="json-view">{{ JSON.stringify(draft, null, 2) }}</pre>
      </a-tab-pane>
    </a-tabs>

    <!-- 字段配置弹窗 -->
    <a-modal v-model:open="configOpen" :title="configField ? `配置字段：${configField.displayName || configField.name}` : '配置字段'">
      <a-form v-if="configField" layout="vertical">
        <a-form-item v-if="configField.type === 'string'" label="长度">
          <a-input-number v-model:value="configField.length" :min="1" style="width: 100%" />
        </a-form-item>
        <template v-if="configField.type === 'decimal'">
          <a-form-item label="精度">
            <a-input-number v-model:value="configField.precision" :min="1" style="width: 100%" />
          </a-form-item>
          <a-form-item label="小数位数">
            <a-input-number v-model:value="configField.scale" :min="0" style="width: 100%" />
          </a-form-item>
        </template>
        <template v-if="configField.type === 'enum'">
          <a-form-item label="选项">
            <div v-for="(opt, oi) in configField.options ?? []" :key="oi" class="group-row">
              <a-input v-model:value="opt.value" placeholder="值" style="width: 160px" />
              <a-input v-model:value="opt.label" placeholder="显示文字" style="width: 160px" />
              <a-button size="small" danger @click="removeEnumOption(configField, oi)">删除</a-button>
            </div>
            <a-button size="small" @click="addOption">添加选项</a-button>
          </a-form-item>
        </template>
        <template v-if="configField.type === 'manyToOne'">
          <a-form-item label="关联模型">
            <a-select v-model:value="relationOf(configField).model">
              <a-select-option v-for="m in allModels" :key="m.name" :value="m.name">{{ m.displayName }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="显示字段">
            <a-select v-model:value="relationOf(configField).displayField">
              <a-select-option v-for="f in displayFieldsOf(relationOf(configField).model)" :key="f.name" :value="f.name">
                {{ f.displayName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <template v-if="configField.type === 'oneToMany'">
          <a-form-item label="子表模型">
            <a-select v-model:value="relationOf(configField).model">
              <a-select-option v-for="m in allModels" :key="m.name" :value="m.name">{{ m.displayName }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="回指字段">
            <a-select v-model:value="relationOf(configField).mappedBy">
              <a-select-option v-for="f in mappedByFieldsOf(relationOf(configField).model)" :key="f.name" :value="f.name">
                {{ f.displayName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </template>
        <a-form-item v-if="showDefaultValue(configField)" label="默认值">
          <component
            :is="fieldComponents[configField.type] as Component"
            :model-value="configField.defaultValue"
            :field="configField"
            @update:model-value="onConfigDefaultChange"
          />
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button type="primary" @click="configOpen = false">确定</a-button>
      </template>
    </a-modal>
  </a-card>
</template>

<style scoped>
.tab-form {
  max-width: 720px;
}

.hint {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}

.field-header,
.field-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.field-header {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.65);
}

.field-col.name {
  width: 160px;
  flex: none;
}

.field-col.type {
  width: 120px;
  flex: none;
}

.field-col.flag {
  width: 48px;
  flex: none;
}

.field-col.ops {
  flex: 1;
  white-space: nowrap;
}

.field-col.ops :deep(.ant-btn) {
  margin-right: 4px;
}

.add-btn {
  margin-top: 8px;
}

.group-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.service-row {
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 8px;
}

.service-row .script {
  margin-top: 8px;
  font-family: monospace;
}

.json-toolbar {
  margin-bottom: 8px;
}

.json-view {
  background: #f6f8fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 480px;
  overflow: auto;
}
</style>
