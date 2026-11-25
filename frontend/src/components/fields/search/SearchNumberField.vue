<script setup lang="ts">
import type { FieldDef } from '@/types/meta'

defineProps<{ modelValue: unknown; field: FieldDef; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: unknown] }>()
</script>

<template>
  <a-input-number
    :value="(modelValue as number | null)"
    :disabled="disabled"
    @update:value="(v: unknown) => emit('update:modelValue', v)"
  />
</template>
