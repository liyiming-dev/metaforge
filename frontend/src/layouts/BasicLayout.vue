<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import * as AntdIcons from '@ant-design/icons-vue'
import { AppstoreOutlined, HomeOutlined, ToolOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'

import { useMock } from '@/api/request'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const { menus } = storeToRefs(menuStore)

const collapsed = ref(false)

const selectedKeys = computed(() => {
  // 新建/编辑模型页也高亮"模型管理"
  if (route.path.startsWith('/design/models')) {
    return ['/design/models']
  }
  return [route.path]
})
const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : ''))

// 所有分组默认展开；菜单加载完成后设一次，之后用户手动收起由 v-model:open-keys 接管
const openKeys = ref<string[]>([])
watch(
  menus,
  (m) => {
    openKeys.value = [...m.map((node) => node.key), 'design']
  },
  { immediate: true },
)

// 按图标名从 @ant-design/icons-vue 里取组件，取不到就用 AppstoreOutlined 兜底
function iconOf(name?: string): Component {
  if (!name) return AppstoreOutlined
  return (AntdIcons as Record<string, Component>)[name] ?? AppstoreOutlined
}

const onMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}
</script>

<template>
  <a-layout class="layout">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      theme="dark"
      :width="220"
      class="sider"
    >
      <div class="logo">
        <svg width="26" height="26" viewBox="0 0 28 28" fill="none" class="logo-svg">
          <path d="M14 2.5L25 8.5v11L14 25.5L3 19.5v-11L14 2.5z" fill="#3B5BDB" />
          <text
            x="14"
            y="18.5"
            text-anchor="middle"
            font-size="11"
            fill="#fff"
            font-weight="bold"
            font-family="sans-serif"
          >
            M
          </text>
        </svg>
        <span v-if="!collapsed" class="logo-text">MetaForge<em>元铸</em></span>
      </div>
      <a-menu
        mode="inline"
        theme="dark"
        :selected-keys="selectedKeys"
        v-model:open-keys="openKeys"
        @click="onMenuClick"
      >
        <a-menu-item key="/">
          <template #icon><HomeOutlined /></template>
          首页
        </a-menu-item>
        <a-sub-menu v-for="node in menus" :key="node.key">
          <template #icon><component :is="iconOf(node.icon)" /></template>
          <template #title>{{ node.displayName }}</template>
          <a-menu-item v-for="child in node.children" :key="child.path ?? child.key">
            {{ child.displayName }}
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="design">
          <template #icon><ToolOutlined /></template>
          <template #title>平台设计</template>
          <a-menu-item key="/design/modules">模块管理</a-menu-item>
          <a-menu-item key="/design/models">模型管理</a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <span class="title">{{ pageTitle }}</span>
        <a-tag v-if="useMock" color="orange">mock 数据</a-tag>
        <a-tag v-else color="green">真实后端</a-tag>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}

.logo {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-svg {
  flex: none;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.logo-text em {
  font-style: normal;
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.55);
  margin-left: 4px;
}

.header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  line-height: 56px;
}

.title {
  font-size: 16px;
  font-weight: 500;
}

.content {
  padding: 24px;
  background: #f5f7fa;
}
</style>
