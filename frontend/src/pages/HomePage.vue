<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { fetchBackendHealth } from '@/api/health'
import { metaApi } from '@/api/meta'
import { useMock } from '@/api/request'
import type { Health } from '@/types/api'

const checking = ref(true)
const health = ref<Health | null>(null)
const healthError = ref('')
const modelCount = ref<number | null>(null)

async function checkBackend() {
  checking.value = true
  healthError.value = ''
  try {
    health.value = await fetchBackendHealth()
  } catch (error) {
    health.value = null
    healthError.value = error instanceof Error ? error.message : String(error)
  } finally {
    checking.value = false
  }
}

async function loadModelCount() {
  try {
    modelCount.value = (await metaApi.models()).length
  } catch {
    // 后端还没实现 /api/meta/models 时会失败，全局提示已经由 request.ts 弹出
    modelCount.value = null
  }
}

onMounted(() => {
  checkBackend()
  if (useMock) {
    loadModelCount()
  }
})
</script>

<template>
  <a-space direction="vertical" size="large" class="page">
    <a-card title="环境检查">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="后端连接">
          <a-spin v-if="checking" size="small" />
          <a-badge
            v-else-if="health"
            status="success"
            :text="`正常，数据库 ${health.database}，后端时间 ${health.time}`"
          />
          <a-badge v-else status="error" :text="`连不上：${healthError}`" />
          <a-button type="link" size="small" @click="checkBackend">重新检查</a-button>
        </a-descriptions-item>
        <a-descriptions-item label="页面数据来源">
          <template v-if="useMock">
            前端内存 mock
            <span v-if="modelCount !== null">（示例里有 {{ modelCount }} 个模型）</span>
          </template>
          <template v-else>真实后端</template>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card title="这是项目骨架">
      <p>现在只有布局和这一页。菜单、通用列表页、通用表单页和模型设计器都还没做。</p>
      <p>
        每个人要做什么、怎么做、做到什么算完成，写在仓库的
        <code>docs/tasks/</code> 目录里。前后端对接的约定写在 <code>docs/contract/</code> 目录里。
      </p>
    </a-card>
  </a-space>
</template>

<style scoped>
.page {
  width: 100%;
}
</style>
