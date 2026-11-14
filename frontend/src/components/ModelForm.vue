<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Component } from 'vue'
import { message } from 'ant-design-vue'

import { dataApi } from '@/api/data'
import { ApiError } from '@/api/request'
import { useModelStore } from '@/stores/model'
import { fieldComponents } from '@/components/fields'
import type { DataRecord } from '@/types/api'
import type { FieldDef, ModelDef } from '@/types/meta'

const props = defineProps<{
  open: boolean
  model: string
  /** 要编辑的记录 id。没有值表示新建 */
  recordId?: number | null
  /** 固定带上的值：对应的字段不显示在表单里，提交时带上 */
  presetValues?: DataRecord
}>()

const emit = defineEmits<{ 'update:open': [value: boolean]; saved: [record: DataRecord] }>()

const modelStore = useModelStore()

const modelDef = ref<ModelDef | null>(null)
const formState = ref<Record<string, unknown>>({})
const fieldErrors = ref<Record<string, string>>({})
const saving = ref(false)
const formRef = ref<{ validate: () => Promise<unknown> } | null>(null)

const title = computed(() => {
  const displayName = modelDef.value?.displayName ?? ''
  return props.recordId != null ? `编辑${displayName}` : `新增${displayName}`
})

// 过滤后的表单分组：去掉组里写了但模型没有的、oneToMany、presetValues 里的字段
const formGroups = computed(() => {
  const presetKeys = new Set(Object.keys(props.presetValues ?? {}))
  return (modelDef.value?.views.form.groups ?? [])
    .map((group) => {
      const fields: FieldDef[] = []
      for (const name of group.fields) {
        const field = modelDef.value?.fields.find((f) => f.name === name)
        if (!field) continue
        if (field.type === 'oneToMany') continue
        if (presetKeys.has(name)) continue
        fields.push(field)
      }
      return { title: group.title, columns: group.columns, fields }
    })
    .filter((group) => group.fields.length > 0)
})

// 必填规则：required 且不是 boolean 的字段，加一条必填校验
const formRules = computed(() => {
  const rules: Record<string, { required: boolean; message: string; trigger: string }[]> = {}
  for (const group of formGroups.value) {
    for (const field of group.fields) {
      if (field.required && field.type !== 'boolean') {
        rules[field.name] = [{ required: true, message: `请填写${field.displayName}`, trigger: 'change' }]
      }
    }
  }
  return rules
})

function hasComponent(field: FieldDef): boolean {
  return field.type in fieldComponents
}

function fieldComponentOf(field: FieldDef): Component {
  return fieldComponents[field.type] as Component
}

async function initForm(): Promise<void> {
  modelDef.value = await modelStore.get(props.model)
  const state: Record<string, unknown> = {}
  for (const field of modelDef.value.fields) {
    if (field.type === 'oneToMany') continue
    state[field.name] = field.defaultValue ?? null
  }
  if (props.recordId != null) {
    const record = await dataApi.get(props.model, props.recordId)
    for (const field of modelDef.value.fields) {
      if (field.type === 'oneToMany') continue
      if (field.name in record) {
        state[field.name] = record[field.name]
      }
    }
  }
  for (const [key, value] of Object.entries(props.presetValues ?? {})) {
    state[key] = value
  }
  formState.value = state
  fieldErrors.value = {}
}

// 打开时初始化；关闭时清空表单状态
watch(
  () => props.open,
  async (open) => {
    if (open) {
      await initForm()
    } else {
      formState.value = {}
      fieldErrors.value = {}
      modelDef.value = null
    }
  },
)

function onFieldChange(name: string, value: unknown): void {
  formState.value[name] = value
  if (fieldErrors.value[name]) {
    delete fieldErrors.value[name]
  }
}

function isReadonly(field: FieldDef): boolean {
  return field.readonly === true
}

function showAutoHint(field: FieldDef): boolean {
  if (field.readonly !== true || props.recordId != null) return false
  const value = formState.value[field.name]
  return value === null || value === undefined || value === ''
}

function onClose(): void {
  emit('update:open', false)
}

async function onSubmit(): Promise<void> {
  if (formRef.value) {
    try {
      await formRef.value.validate()
    } catch {
      return // 校验不通过
    }
  }
  saving.value = true
  try {
    // 提交所有非 oneToMany 字段的当前值；manyToOne 的 { id, label } 只提交 id
    const data: DataRecord = {}
    for (const field of modelDef.value?.fields ?? []) {
      if (field.type === 'oneToMany') continue
      const value = formState.value[field.name]
      if (field.type === 'manyToOne' && value && typeof value === 'object' && 'id' in value) {
        data[field.name] = (value as { id: unknown }).id
      } else {
        data[field.name] = value
      }
    }
    const saved =
      props.recordId != null
        ? await dataApi.update(props.model, props.recordId, data)
        : await dataApi.create(props.model, data)
    message.success('已保存')
    emit('saved', saved)
    emit('update:open', false)
  } catch (error) {
    if (error instanceof ApiError) {
      // 有字段级错误时显示到对应表单项；没有时什么都不用做（request 已经弹过全局提示）
      if (Object.keys(error.fieldErrors).length > 0) {
        fieldErrors.value = error.fieldErrors
      }
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <a-drawer :open="open" :width="720" :title="title" :mask-closable="false" @close="onClose">
    <a-form ref="formRef" :model="formState" :rules="formRules" layout="vertical">
      <template v-for="group in formGroups" :key="group.title">
        <div class="group-title">{{ group.title }}</div>
        <a-row :gutter="16">
          <a-col v-for="field in group.fields" :key="field.name" :span="24 / group.columns">
            <a-form-item
              :label="field.displayName"
              :name="field.name"
              :validate-status="fieldErrors[field.name] ? 'error' : undefined"
              :help="fieldErrors[field.name]"
              :extra="showAutoHint(field) ? '保存后自动生成' : undefined"
            >
              <component
                v-if="hasComponent(field)"
                :is="fieldComponentOf(field)"
                :model-value="formState[field.name]"
                :field="field"
                :disabled="isReadonly(field)"
                @update:model-value="(v: unknown) => onFieldChange(field.name, v)"
              />
              <a-input v-else disabled placeholder="这种字段在任务 F4 实现" />
            </a-form-item>
          </a-col>
        </a-row>
      </template>
    </a-form>

    <template #footer>
      <a-space>
        <a-button @click="onClose">取消</a-button>
        <a-button type="primary" :loading="saving" @click="onSubmit">保存</a-button>
      </a-space>
    </template>
  </a-drawer>
</template>

<style scoped>
.group-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
</style>
