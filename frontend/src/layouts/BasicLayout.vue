<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import * as AntdIcons from '@ant-design/icons-vue'
import { AppstoreOutlined, HomeOutlined } from '@ant-design/icons-vue'
import { storeToRefs } from 'pinia'

import { useMock } from '@/api/request'
import { useMenuStore } from '@/stores/menu'

const route = useRoute()
const router = useRouter()
const menuStore = useMenuStore()
const { menus } = storeToRefs(menuStore)

const selectedKeys = computed(() => [route.path])
const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : ''))

// 所有分组默认展开；菜单加载完成后设一次，之后用户手动收起由 v-model:open-keys 接管
const openKeys = ref<string[]>([])
watch(
  menus,
  (m) => {
    openKeys.value = m.map((node) => node.key)
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
    <a-layout-sider theme="light" :width="220" class="sider">
      <div class="logo">MetaForge<span>元铸</span></div>
      <a-menu
        mode="inline"
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

.sider {
  border-right: 1px solid #f0f0f0;
}

.logo {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.logo span {
  font-size: 13px;
  font-weight: 400;
  color: rgba(0, 0, 0, 0.45);
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
}
</style>
