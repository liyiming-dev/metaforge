<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <a-result status="404" title="页面不存在" sub-title="地址可能写错了，或者对应的模型已经被删除。">
    <template #extra>
      <a-button type="primary" @click="router.push('/')">回到首页</a-button>
    </template>
  </a-result>
</template>
