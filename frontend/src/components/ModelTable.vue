<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import type { TableColumnsType, TablePaginationConfig } from 'ant-design-vue'

import { dataApi } from '@/api/data'
import { useModelStore } from '@/stores/model'
import { SYSTEM_FIELD_TITLES, displayValue } from '@/components/fields/display'
import ModelForm from '@/components/ModelForm.vue'
import ManyToOneField from '@/components/fields/ManyToOneField.vue'
import type { DataRecord, Filter } from '@/types/api'
import type { FieldDef, ModelDef, SortItem } from '@/types/meta'

const props = defineProps<{
  /** 模型名 */
  model: string
  /** 固定的过滤条件：每次查询都带上，用户看不到也改不了。任务 F4 的子表会用 */
  fixedFilters?: Filter[]
  /** 不显示的列，同时也不出现在搜索区。任务 F4 的子表会用 */
  hiddenColumns?: string[]
  /** 新建记录时固定带上的值。任务 F3 会把它传给表单 */
  presetValues?: DataRecord
}>()

const modelStore = useModelStore()

const modelDef = ref<ModelDef | null>(null)
const records = ref<DataRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const sort = ref<SortItem[]>([])
const loading = ref(false)

/** 搜索区各字段的当前值，键是字段名 */
const searchValues = reactive<Record<string, unknown>>({})

const SYSTEM_FIELDS = ['id', 'created_at', 'updated_at']

function isSystemField(name: string): boolean {
  return SYSTEM_FIELDS.includes(name)
}

function fieldByName(name: string): FieldDef | undefined {
  return modelDef.value?.fields.find((f) => f.name === name)
}

function fieldType(name: string): string | undefined {
  return fieldByName(name)?.type
}

/** 取字段定义（调用方已确认字段存在，这里做非空断言方便传参） */
function fieldOf(name: string): FieldDef {
  return fieldByName(name) as FieldDef
}

const fieldNames = computed(() => new Set(modelDef.value?.fields.map((f) => f.name) ?? []))

/** 列表要显示的列：views.list.columns 去掉 hiddenColumns 和无效字段 */
const listColumns = computed(() => {
  const hidden = new Set(props.hiddenColumns ?? [])
  return (modelDef.value?.views.list.columns ?? []).filter(
    (name) => !hidden.has(name) && (fieldNames.value.has(name) || isSystemField(name)),
  )
})

/** 搜索区要显示的字段：去掉 hiddenColumns */
const searchFieldNames = computed(() => {
  const hidden = new Set(props.hiddenColumns ?? [])
  return (modelDef.value?.views.list.searchFields ?? []).filter((name) => {
    if (hidden.has(name)) return false
    return !!fieldByName(name)
  })
})

const hasSearch = computed(() => searchFieldNames.value.length > 0)

function columnTitle(name: string): string {
  const field = fieldByName(name)
  return field ? field.displayName : (SYSTEM_FIELD_TITLES[name] ?? name)
}

/** 除了 text、manyToOne，其它列都可以点列头排序（系统字段也可以） */
function sortable(name: string): boolean {
  const field = fieldByName(name)
  return !field || !['text', 'manyToOne'].includes(field.type)
}

const tableColumns = computed<TableColumnsType>(() => {
  const cols: TableColumnsType = listColumns.value.map((name) => ({
    title: columnTitle(name),
    dataIndex: name,
    key: name,
    sorter: sortable(name),
  }))
  cols.push({ title: '操作', key: 'actions', width: 140 })
  return cols
})

function enumOptions(name: string): { value: string; label: string }[] {
  return fieldByName(name)?.options ?? []
}

/** 组装本次查询的过滤条件：固定条件 + 搜索区里用户填了的条件 */
function buildFilters(): Filter[] {
  const filters: Filter[] = [...(props.fixedFilters ?? [])]
  for (const name of searchFieldNames.value) {
    const field = fieldByName(name)
    if (!field) continue
    const value = searchValues[name]
    if (value === null || value === undefined || value === '') continue
    switch (field.type) {
      case 'string':
      case 'text':
        filters.push({ field: name, op: 'like', value })
        break
      case 'enum':
      case 'boolean':
      case 'integer':
      case 'decimal':
        filters.push({ field: name, op: 'eq', value })
        break
      case 'date':
      case 'datetime': {
        const range = value as [string, string] | null
        if (!range || range.length !== 2) break
        const [start, end] = range
        if (start) {
          filters.push({
            field: name,
            op: 'ge',
            value: field.type === 'datetime' ? `${start} 00:00:00` : start,
          })
        }
        if (end) {
          filters.push({
            field: name,
            op: 'le',
            value: field.type === 'datetime' ? `${end} 23:59:59` : end,
          })
        }
        break
      }
      case 'manyToOne': {
        // 值可能是 { id, label }，也可能是纯 id，统一取 id 再比较
        const id = value && typeof value === 'object' ? (value as { id: unknown }).id : value
        filters.push({ field: name, op: 'eq', value: id })
        break
      }
    }
  }
  return filters
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const result = await dataApi.search(props.model, {
      page: page.value,
      size: size.value,
      filters: buildFilters(),
      sort: sort.value.length > 0 ? sort.value : undefined,
    })
    records.value = result.records
    total.value = result.total
  } catch {
    // 失败时 request.ts 已经弹过全局提示，这里保持列表不变即可
  } finally {
    loading.value = false
  }
}

function resetQueryState(): void {
  page.value = 1
  size.value = modelDef.value?.views.list.pageSize ?? 10
  sort.value = []
  for (const key of Object.keys(searchValues)) {
    delete searchValues[key]
  }
}

// model 变化时重新取定义，并重置搜索条件、排序和页码
watch(
  () => props.model,
  async () => {
    await loadModel()
    resetQueryState()
    await load()
  },
)

async function loadModel(): Promise<void> {
  modelDef.value = await modelStore.get(props.model)
}

onMounted(async () => {
  await loadModel()
  resetQueryState()
  await load()
})

function onSearch(): void {
  page.value = 1
  load()
}

function onReset(): void {
  for (const name of searchFieldNames.value) {
    delete searchValues[name]
  }
  page.value = 1
  load()
}

function onTableChange(pagination: TablePaginationConfig, _filters: unknown, sorter: unknown): void {
  const s = (Array.isArray(sorter) ? sorter[0] : sorter) as
    | { field?: string | number; order?: 'ascend' | 'descend' }
    | undefined
  const newSort: SortItem[] = s?.order
    ? [{ field: String(s.field ?? ''), order: s.order === 'ascend' ? 'asc' : 'desc' }]
    : []
  const sortChanged = JSON.stringify(newSort) !== JSON.stringify(sort.value)
  sort.value = newSort
  if (sortChanged) {
    // 排序变了，回到第 1 页重新查
    page.value = 1
  } else {
    page.value = pagination.current ?? 1
    size.value = pagination.pageSize ?? size.value
  }
  load()
}

const paginationConfig = computed<TablePaginationConfig>(() => ({
  current: page.value,
  pageSize: size.value,
  total: total.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (t: number) => `共 ${t} 条`,
}))

const formOpen = ref(false)
const editingId = ref<number | null>(null)

function onCreate(): void {
  editingId.value = null
  formOpen.value = true
}

function onEdit(record: DataRecord): void {
  editingId.value = record.id as number
  formOpen.value = true
}

function onSaved(): void {
  load()
}

async function onDelete(record: DataRecord): Promise<void> {
  try {
    await dataApi.remove(props.model, record.id as number)
    message.success('已删除')
    // 当前页删完没数据了、又不是第 1 页时，回到上一页
    if (records.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await load()
  } catch {
    // 失败时 request.ts 已经弹过全局提示，列表保持不变
  }
}

defineExpose({ reload: load })
</script>

<template>
  <div class="model-table">
    <a-form v-if="hasSearch" layout="inline" class="search-bar">
      <a-form-item v-for="name in searchFieldNames" :key="name" :label="columnTitle(name)">
        <a-input
          v-if="fieldType(name) === 'string' || fieldType(name) === 'text'"
          v-model:value="searchValues[name]"
          allow-clear
          placeholder="请输入"
          style="width: 160px"
        />
        <a-select
          v-else-if="fieldType(name) === 'enum'"
          v-model:value="searchValues[name]"
          allow-clear
          placeholder="请选择"
          style="width: 160px"
        >
          <a-select-option v-for="opt in enumOptions(name)" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-select-option>
        </a-select>
        <a-select
          v-else-if="fieldType(name) === 'boolean'"
          v-model:value="searchValues[name]"
          allow-clear
          placeholder="请选择"
          style="width: 120px"
        >
          <a-select-option :value="true">是</a-select-option>
          <a-select-option :value="false">否</a-select-option>
        </a-select>
        <a-input-number
          v-else-if="fieldType(name) === 'integer' || fieldType(name) === 'decimal'"
          v-model:value="searchValues[name]"
          style="width: 140px"
        />
        <a-range-picker
          v-else-if="fieldType(name) === 'date' || fieldType(name) === 'datetime'"
          v-model:value="searchValues[name]"
          value-format="YYYY-MM-DD"
        />
        <ManyToOneField
          v-else-if="fieldType(name) === 'manyToOne'"
          :model-value="searchValues[name]"
          :field="fieldOf(name)"
          @update:model-value="(v: unknown) => (searchValues[name] = v)"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="onSearch">查询</a-button>
        <a-button class="reset-btn" @click="onReset">重置</a-button>
      </a-form-item>
    </a-form>

    <div class="toolbar">
      <a-button type="primary" @click="onCreate">新增</a-button>
    </div>

    <a-table
      :columns="tableColumns"
      :data-source="records"
      :loading="loading"
      :pagination="paginationConfig"
      row-key="id"
      @change="onTableChange"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.key === 'actions'">
          <a-button type="link" size="small" @click="onEdit(record)">编辑</a-button>
          <a-popconfirm title="确定删除这条记录吗？" @confirm="onDelete(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </template>
        <template v-else-if="fieldType(String(column.key)) === 'enum'">
          <a-tag>{{ displayValue(fieldByName(String(column.key)), text) }}</a-tag>
        </template>
        <template v-else-if="fieldType(String(column.key)) === 'boolean'">
          <a-tag :color="text ? 'green' : undefined">
            {{ displayValue(fieldByName(String(column.key)), text) }}
          </a-tag>
        </template>
        <template v-else>
          {{ displayValue(fieldByName(String(column.key)), text) }}
        </template>
      </template>
    </a-table>

    <ModelForm
      :open="formOpen"
      :model="model"
      :record-id="editingId"
      :preset-values="presetValues"
      @update:open="formOpen = $event"
      @saved="onSaved"
    />
  </div>
</template>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.reset-btn {
  margin-left: 8px;
}
</style>
