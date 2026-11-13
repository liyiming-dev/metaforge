import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import BasicLayout from '@/layouts/BasicLayout.vue'

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
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/pages/NotFoundPage.vue'),
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
