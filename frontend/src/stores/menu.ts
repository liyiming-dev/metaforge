// 菜单 store：根据 /api/meta/menus 的返回，在运行时给业务模型注册动态路由。
// 业务模型的页面地址（/app/material 这类）不写在固定路由表里，而是由这里按菜单逐个 addRoute。
// 注意：本文件 import @/router，而 src/router/index.ts 也 import 本 store，
// 两者互相 import 是安全的，因为都只在函数内部使用对方，没有在模块顶层立即调用。
import { ref } from 'vue'
import { defineStore } from 'pinia'

import type { MenuNode } from '@/types/api'
import { metaApi } from '@/api/meta'
import router from '@/router'

export const useMenuStore = defineStore('menu', () => {
  /** 接口返回的菜单树 */
  const menus = ref<MenuNode[]>([])
  /** 是否已经尝试加载过菜单（成功或失败都算），用于路由守卫判断 */
  const loaded = ref(false)
  /** 动态注册过的路由名，reload 时要先按名移除，避免同名路由重复注册 */
  const registeredNames = ref<string[]>([])

  /** 递归注册菜单里的叶子节点（同时有 model 和 path 的节点）为路由 */
  function register(node: MenuNode): void {
    if (node.model && node.path) {
      const name = `app-${node.model}`
      router.addRoute('layout', {
        path: node.path,
        name,
        component: () => import('@/pages/ModelListPage.vue'),
        props: { model: node.model },
        meta: { title: node.displayName },
      })
      registeredNames.value.push(name)
    }
    for (const child of node.children ?? []) {
      register(child)
    }
  }

  async function load(): Promise<void> {
    try {
      menus.value = await metaApi.menus()
      for (const node of menus.value) {
        register(node)
      }
    } catch {
      // 加载失败也要把 loaded 置为 true、menus 清空，
      // 否则路由守卫会反复重试、陷入死循环。
      menus.value = []
    } finally {
      loaded.value = true
    }
  }

  async function reload(): Promise<void> {
    for (const name of registeredNames.value) {
      router.removeRoute(name)
    }
    registeredNames.value = []
    await load()
  }

  return { menus, loaded, load, reload }
})
