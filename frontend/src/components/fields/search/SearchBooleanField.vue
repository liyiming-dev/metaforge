<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()
</script>

<template>
  <a-select
    :value="(modelValue as boolean | null)"
    allow-clear
    placeholder="请选择"
    :disabled="disabled"
    @update:value="(v: unknown) => emit('update:modelValue', v)"
  >
    <a-select-option :value="true">是</a-select-option>
    <a-select-option :value="false">否</a-select-option>
  </a-select>
</template>
