import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import BasicLayout from '@/layouts/BasicLayout.vue'
import { useMenuStore } from '@/stores/menu'

// 这里只放固定的路由。
// 业务模型的页面（/app/material 这类）不写在这里：前端任务 F1 要根据 /api/meta/menus 的返回，
// 用 router.addRoute('layout', ...) 把它们动态加到名为 layout 的路由下面。
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'layout',
    component: BasicLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/pages/HomePage.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'design/modules',
        name: 'design-modules',
        component: () => import('@/pages/design/ModuleListPage.vue'),
        meta: { title: '模块管理' },
      },
      {
        path: 'design/models',
        name: 'design-models',
        component: () => import('@/pages/design/ModelDesignListPage.vue'),
        meta: { title: '模型管理' },
      },
      {
        path: 'design/models/new',
        name: 'design-model-new',
        component: () => import('@/pages/design/ModelEditPage.vue'),
        meta: { title: '新建模型' },
      },
      {
        path: 'design/models/:name',
        name: 'design-model-edit',
        component: () => import('@/pages/design/ModelEditPage.vue'),
        props: true,
        meta: { title: '编辑模型' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/NotFoundPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局前置守卫：首次导航时先把菜单加载出来、注册好动态路由，
// 再让这次跳转重新匹配一遍。这样刷新 /app/material 这类地址时不会落到 404。
router.beforeEach(async (to) => {
  const menuStore = useMenuStore()
  if (!menuStore.loaded) {
    await menuStore.load()
    return to.fullPath
  }
})

export default router
