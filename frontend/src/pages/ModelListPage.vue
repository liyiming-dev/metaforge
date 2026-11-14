<script setup lang="ts">
// 通用列表页：接收模型名，渲染通用的 ModelTable 组件。
// 列、搜索区、排序都来自模型定义里的 views.list，这里不写任何具体模型。
import ModelTable from '@/components/ModelTable.vue'

defineProps<{ model: string }>()
</script>

<template>
  <ModelTable :model="model" />
</template>
