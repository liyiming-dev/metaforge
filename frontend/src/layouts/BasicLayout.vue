<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
import { HomeOutlined } from '@ant-design/icons-vue'

import { useMock } from '@/api/request'

const route = useRoute()
const router = useRouter()

const selectedKeys = computed(() => [route.path])
const pageTitle = computed(() => (typeof route.meta.title === 'string' ? route.meta.title : ''))

const onMenuClick: MenuProps['onClick'] = ({ key }) => {
  router.push(String(key))
}
</script>

<template>
  <a-layout class="layout">
    <a-layout-sider theme="light" :width="220" class="sider">
      <div class="logo">MetaForge<span>元铸</span></div>
      <a-menu mode="inline" :selected-keys="selectedKeys" @click="onMenuClick">
        <a-menu-item key="/">
          <template #icon><HomeOutlined /></template>
          首页
        </a-menu-item>
        <!-- 前端任务 F1：在这里渲染 /api/meta/menus 返回的动态菜单 -->
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
