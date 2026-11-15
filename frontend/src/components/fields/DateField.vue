<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()

function onUpdate(value: string | null): void {
  emit('update:modelValue', value)
}
</script>

<template>
  <a-date-picker
    :value="(modelValue as string | null)"
    :value-format="field.type === 'datetime' ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD'"
    :show-time="field.type === 'datetime'"
    :disabled="disabled"
    style="width: 100%"
    @update:value="onUpdate"
  />
</template>
