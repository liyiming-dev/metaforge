<script setup lang="ts">
import { computed, ref } from 'vue'

import { dataApi } from '@/api/data'
import type { OptionItem, RefValue } from '@/types/api'
import type { FieldDef } from '@/types/meta'

const props = defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

const options = ref<OptionItem[]>([])
const loading = ref(false)
const keyword = ref('')
let timer: ReturnType<typeof setTimeout> | undefined

const relation = computed(() => props.field.relation)

// label-in-value 模式下，a-select 的 value 是 { value, label } 对象；
// 这里把 RefValue（{ id, label }）转成它，{ id, label } 能直接显示 label，不用等请求。
const selectValue = computed(() => {
  const v = props.modelValue
  if (v === null || v === undefined) return undefined
  if (typeof v === 'object' && 'id' in v) {
    const ref = v as RefValue
    return { value: ref.id, label: ref.label }
  }
  return { value: Number(v), label: String(v) }
})

async function fetchOptions(kw: string): Promise<void> {
  if (!relation.value?.model) return
  loading.value = true
  try {
    options.value = await dataApi.options(relation.value.model, {
      keyword: kw || undefined,
      displayField: relation.value.displayField,
      limit: 20,
    })
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

function onSearch(kw: string): void {
  keyword.value = kw
  if (timer) clearTimeout(timer)
  // 输入时带关键字重新请求，加 300 毫秒防抖
  timer = setTimeout(() => fetchOptions(kw), 300)
}

function onChange(value: unknown): void {
  if (value === null || value === undefined) {
    emit('update:modelValue', null)
    return
  }
  const v = value as { value: number; label: string }
  emit('update:modelValue', { id: v.value, label: v.label } as RefValue)
}
</script>

<template>
  <a-select
    :value="selectValue"
    :options="options"
    label-in-value
    show-search
    :filter-option="false"
    allow-clear
    :loading="loading"
    :disabled="disabled"
    placeholder="输入关键字搜索"
    style="width: 100%"
    @search="onSearch"
    @change="onChange"
    @dropdown-visible-change="(open: boolean) => open && fetchOptions(keyword)"
  />
</template>
