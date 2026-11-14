<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: boolean): void {
  emit('update:modelValue', value)
}
</script>

<template>
  <a-switch :checked="modelValue === true" :disabled="disabled" @update:checked="onUpdate" />
</template>
