<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { Component } from 'vue'
import { message } from 'ant-design-vue'
import {
  AppstoreOutlined,
  DatabaseOutlined,
  SettingOutlined,
  ShopOutlined,
  TeamOutlined,
  ToolOutlined,
} from '@ant-design/icons-vue'

import { metaApi } from '@/api/meta'
import { useMenuStore } from '@/stores/menu'
import type { ModuleDef } from '@/types/meta'

const menuStore = useMenuStore()

const modules = ref<ModuleDef[]>([])
const loading = ref(false)

const modalOpen = ref(false)
const editing = ref(false)
const saving = ref(false)
const formState = reactive({
  name: '',
  displayName: '',
  icon: 'AppstoreOutlined',
  sort: 100,
})

const NAME_PATTERN = /^[a-z][a-z0-9_]{0,29}$/

const iconOptions: { value: string; component: Component }[] = [
  { value: 'AppstoreOutlined', component: AppstoreOutlined },
  { value: 'SettingOutlined', component: SettingOutlined },
  { value: 'DatabaseOutlined', component: DatabaseOutlined },
  { value: 'ShopOutlined', component: ShopOutlined },
  { value: 'ToolOutlined', component: ToolOutlined },
  { value: 'TeamOutlined', component: TeamOutlined },
]

const columns = [
  { title: '标识', dataIndex: 'name', key: 'name' },
  { title: '显示名', dataIndex: 'displayName', key: 'displayName' },
  { title: '图标', dataIndex: 'icon', key: 'icon' },
  { title: '排序', dataIndex: 'sort', key: 'sort' },
  { title: '操作', key: 'actions', width: 140 },
]

function iconOf(name?: string): Component {
  return iconOptions.find((o) => o.value === name)?.component ?? AppstoreOutlined
}

async function load(): Promise<void> {
  loading.value = true
  try {
    modules.value = await metaApi.modules()
  } catch {
    // 失败提示由 request.ts 弹
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate(): void {
  editing.value = false
  formState.name = ''
  formState.displayName = ''
  formState.icon = 'AppstoreOutlined'
  formState.sort = 100
  modalOpen.value = true
}

function openEdit(module: ModuleDef): void {
  editing.value = true
  formState.name = module.name
  formState.displayName = module.displayName
  formState.icon = module.icon ?? 'AppstoreOutlined'
  formState.sort = module.sort ?? 100
  modalOpen.value = true
}

async function onSave(): Promise<void> {
  if (!formState.displayName.trim()) {
    message.error('显示名不能为空')
    return
  }
  if (!editing.value && !NAME_PATTERN.test(formState.name)) {
    message.error('标识要小写字母开头，只含小写字母、数字、下划线，最长 30 位')
    return
  }
  saving.value = true
  try {
    await metaApi.saveModule({
      name: formState.name,
      displayName: formState.displayName,
      icon: formState.icon,
      sort: formState.sort,
    })
    message.success('已保存')
    modalOpen.value = false
    await load()
    await menuStore.reload()
  } catch {
    // 失败提示由 request.ts 弹
  } finally {
    saving.value = false
  }
}

async function onRemove(module: ModuleDef): Promise<void> {
  try {
    await metaApi.removeModule(module.name)
    message.success('已删除')
    await load()
    await menuStore.reload()
  } catch {
    // 失败提示由 request.ts 弹（比如模块下还有模型）
  }
}
</script>

<template>
  <a-card title="模块管理" :bordered="false">
    <template #extra>
      <a-button type="primary" @click="openCreate">新增模块</a-button>
    </template>

    <a-table :columns="columns" :data-source="modules" :loading="loading" row-key="name" :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'icon'">
          <component :is="iconOf(record.icon)" />
        </template>
        <template v-else-if="column.key === 'actions'">
          <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
          <a-popconfirm title="确定删除这个模块吗？" @confirm="onRemove(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="modalOpen" :title="editing ? '编辑模块' : '新增模块'">
      <a-form layout="vertical">
        <a-form-item label="标识" :required="!editing">
          <a-input
            v-model:value="formState.name"
            :disabled="editing"
            placeholder="小写字母开头，只含小写字母、数字、下划线，最长 30 位"
          />
        </a-form-item>
        <a-form-item label="显示名" required>
          <a-input v-model:value="formState.displayName" />
        </a-form-item>
        <a-form-item label="图标">
          <a-select v-model:value="formState.icon">
            <a-select-option v-for="opt in iconOptions" :key="opt.value" :value="opt.value">
              <component :is="opt.component" />
              <span class="icon-label">{{ opt.value }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="formState.sort" style="width: 100%" />
        </a-form-item>
      </a-form>

      <template #footer>
        <a-button @click="modalOpen = false">取消</a-button>
        <a-button type="primary" :loading="saving" @click="onSave">确定</a-button>
      </template>
    </a-modal>
  </a-card>
</template>

<style scoped>
.icon-label {
  margin-left: 8px;
}
</style>
