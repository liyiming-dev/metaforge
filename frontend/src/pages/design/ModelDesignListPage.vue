<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

import { metaApi } from '@/api/meta'
import { useMenuStore } from '@/stores/menu'
import { useModelStore } from '@/stores/model'
import type { MetaBundleInput } from '@/types/api'
import type { ModelDef, ModuleDef } from '@/types/meta'

const router = useRouter()
const menuStore = useMenuStore()
const modelStore = useModelStore()

const models = ref<ModelDef[]>([])
const modules = ref<ModuleDef[]>([])
const loading = ref(false)

const columns = [
  { title: '标识', dataIndex: 'name', key: 'name' },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
  { title: '所属模块', key: 'module' },
  { title: '字段数', key: 'fieldCount' },
  { title: '显示在菜单', key: 'showInMenu' },
  { title: '操作', key: 'actions', width: 200 },
]

function moduleDisplayName(name: string): string {
  return modules.value.find((m) => m.name === name)?.displayName ?? name
}

async function load(): Promise<void> {
  loading.value = true
  try {
    const [ms, mds] = await Promise.all([metaApi.models(), metaApi.modules()])
    models.value = ms
    modules.value = mds
  } catch {
    // 失败提示由 request.ts 弹
  } finally {
    loading.value = false
  }
}

onMounted(load)

function goCreate(): void {
  router.push('/design/models/new')
}

function goEdit(model: ModelDef): void {
  router.push(`/design/models/${model.name}`)
}

function goData(model: ModelDef): void {
  router.push(`/app/${model.name}`)
}

async function onRemove(model: ModelDef): Promise<void> {
  try {
    await metaApi.removeModel(model.name)
    message.success('已删除')
    await load()
    await menuStore.reload()
    modelStore.invalidate(model.name)
  } catch {
    // 失败提示由 request.ts 弹（比如被关联字段引用）
  }
}

async function onExport(): Promise<void> {
  try {
    const bundle = await metaApi.exportAll()
    const json = JSON.stringify(bundle, null, 2)
    const blob = new Blob([json], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'metaforge-meta.json'
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 失败提示由 request.ts 弹
  }
}

const importOpen = ref(false)
const importText = ref('')
const importPreview = ref<{ modules: number; models: number; names: string[] } | null>(null)
const importing = ref(false)

function parseImport(): void {
  try {
    const parsed = JSON.parse(importText.value) as MetaBundleInput
    const moduleCount = parsed.modules?.length ?? 0
    const modelCount = parsed.models?.length ?? 0
    if (moduleCount === 0 && modelCount === 0) {
      message.error('没有可导入的内容')
      importPreview.value = null
      return
    }
    importPreview.value = {
      modules: moduleCount,
      models: modelCount,
      names: parsed.models?.map((m) => m.displayName ?? m.name) ?? [],
    }
  } catch {
    message.error('不是合法的 JSON')
    importPreview.value = null
  }
}

function handleFile(file: File): false {
  const reader = new FileReader()
  reader.onload = () => {
    importText.value = String(reader.result ?? '')
    parseImport()
  }
  reader.readAsText(file)
  return false
}

async function onImport(): Promise<void> {
  if (!importPreview.value) return
  importing.value = true
  try {
    const parsed = JSON.parse(importText.value) as MetaBundleInput
    const result = await metaApi.importBundle(parsed)
    message.success(`导入成功：${result.modules} 个模块，${result.models} 个模型`)
    importOpen.value = false
    importText.value = ''
    importPreview.value = null
    await load()
    await menuStore.reload()
    modelStore.invalidate()
  } catch {
    // 失败提示由 request.ts 弹
  } finally {
    importing.value = false
  }
}
</script>

<template>
  <a-card title="模型管理" :bordered="false">
    <template #extra>
      <a-space>
        <a-button type="primary" @click="goCreate">新建模型</a-button>
        <a-button @click="onExport">导出</a-button>
        <a-button @click="importOpen = true">导入</a-button>
      </a-space>
    </template>

    <a-table :columns="columns" :data-source="models" :loading="loading" row-key="name" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'module'">{{ moduleDisplayName(record.module) }}</template>
        <template v-else-if="column.key === 'fieldCount'">{{ record.fields?.length ?? 0 }}</template>
        <template v-else-if="column.key === 'showInMenu'">{{ record.showInMenu === false ? '否' : '是' }}</template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" size="small" @click="goEdit(record)">编辑</a-button>
          <a-button type="link" size="small" :disabled="record.showInMenu === false" @click="goData(record)">
            数据
          </a-button>
          <a-popconfirm title="确定删除这个模型吗？" @confirm="onRemove(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="importOpen" title="导入">
      <a-space direction="vertical" style="width: 100%">
        <a-upload :before-upload="handleFile" :show-upload-list="false" accept=".json">
          <a-button>选择 .json 文件</a-button>
        </a-upload>
        <a-textarea v-model:value="importText" :rows="8" placeholder="或者在这里粘贴 JSON 文本" />
        <a-button @click="parseImport">解析</a-button>
        <div v-if="importPreview" class="import-preview">
          将导入 {{ importPreview.modules }} 个模块、{{ importPreview.models }} 个模型：
          <template v-if="importPreview.names.length">{{ importPreview.names.join('、') }}</template>
        </div>
      </a-space>

      <template #footer>
        <a-button @click="importOpen = false">取消</a-button>
        <a-button type="primary" :loading="importing" :disabled="!importPreview" @click="onImport">
          确认导入
        </a-button>
      </template>
    </a-modal>
  </a-card>
</template>

<style scoped>
.import-preview {
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}
</style>
