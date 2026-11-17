<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: string | null): void {
  emit('update:modelValue', value)
}
</script>

<template>
  <a-select
    :value="(modelValue as string | null)"
    :allow-clear="!field.required"
    :disabled="disabled"
    placeholder="请选择"
    style="width: 100%"
    @update:value="onUpdate"
  >
    <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
      {{ opt.label }}
    </a-select-option>
  </a-select>
</template>
