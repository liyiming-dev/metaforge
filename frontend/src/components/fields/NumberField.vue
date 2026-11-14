<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: number | null): void {
  emit('update:modelValue', value)
}
</script>

<template>
  <a-input-number
    :value="(modelValue as number | null)"
    :precision="field.type === 'decimal' ? (field.scale ?? 4) : 0"
    :disabled="disabled"
    style="width: 100%"
    @update:value="onUpdate"
  />
</template>
