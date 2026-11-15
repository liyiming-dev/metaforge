<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { fetchBackendHealth } from '@/api/health'
import { metaApi } from '@/api/meta'
import { useMock } from '@/api/request'
import type { Health } from '@/types/api'
import type { ModelDef } from '@/types/meta'

const router = useRouter()

const checking = ref(true)
const health = ref<Health | null>(null)
const healthError = ref('')
const moduleCount = ref<number | null>(null)
const modelCount = ref<number | null>(null)
const fieldCount = ref<number | null>(null)
const menuModels = ref<ModelDef[]>([])

async function checkBackend(): Promise<void> {
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

async function loadStats(): Promise<void> {
  try {
    const models = await metaApi.models()
    modelCount.value = models.length
    fieldCount.value = models.reduce((sum, m) => sum + m.fields.length, 0)
    menuModels.value = models.filter((m) => m.showInMenu !== false)
    moduleCount.value = (await metaApi.modules()).length
  } catch {
    // 后端还没实现相关接口时会失败，全局提示已经由 request.ts 弹出
    modelCount.value = null
    fieldCount.value = null
    menuModels.value = []
    moduleCount.value = null
  }
}

onMounted(() => {
  checkBackend()
  if (useMock) {
    loadStats()
  }
})

function goModel(model: ModelDef): void {
  router.push(`/app/${model.name}`)
}
</script>

<template>
  <a-space direction="vertical" size="large" class="page">
    <div class="welcome">
      <h2>欢迎使用 MetaForge 元铸</h2>
      <p>一个模型驱动的低代码平台：定义模型、字段、视图和服务，平台自动建表、生成增删改查接口并渲染页面，改定义不用改代码。</p>
    </div>

    <a-row :gutter="16">
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="模块数" :value="moduleCount ?? 0" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="模型数" :value="modelCount ?? 0" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card class="stat-card">
          <a-statistic title="字段总数" :value="fieldCount ?? 0" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="快捷入口">
      <a-row :gutter="16">
        <a-col v-for="model in menuModels" :key="model.name" :span="6">
          <a-card hoverable size="small" class="entry-card" @click="goModel(model)">
            <div class="entry-name">{{ model.displayName }}</div>
            <div class="entry-desc">{{ model.description || model.name }}</div>
          </a-card>
        </a-col>
      </a-row>
    </a-card>

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
  </a-space>
</template>

<style scoped>
.page {
  width: 100%;
}

.welcome h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.welcome p {
  margin: 0;
  color: rgba(0, 0, 0, 0.55);
}

.entry-name {
  font-weight: 600;
}

.entry-desc {
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
