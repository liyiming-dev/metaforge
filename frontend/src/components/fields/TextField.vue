<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: string): void {
  emit('update:modelValue', value === '' ? null : value)
}
</script>

<template>
  <a-textarea
    :value="(modelValue as string | null) ?? ''"
    :rows="3"
    :disabled="disabled"
    @update:value="onUpdate"
  />
</template>
