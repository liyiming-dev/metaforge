<script setup lang="ts">
// 通用列表页的占位实现：F1 只接收模型名并显示一句提示，真正的表格在 F2 实现。
defineProps<{ model: string }>()
</script>

<template>
  <a-empty :description="`模型 ${model} 的列表页将在任务 F2 实现`" />
</template>
