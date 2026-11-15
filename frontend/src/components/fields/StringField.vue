<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: string): void {
  // 清空时发出 null，不要发空字符串
  emit('update:modelValue', value === '' ? null : value)
}
</script>

<template>
  <a-input
    :value="(modelValue as string | null) ?? ''"
    :maxlength="field.length ?? 255"
    :disabled="disabled"
    @update:value="onUpdate"
  />
</template>
