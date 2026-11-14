// 模型定义 store：按模型名缓存 metaApi.model(name) 的结果，避免同一个模型反复请求。
import { defineStore } from 'pinia'

import type { ModelDef } from '@/types/meta'
import { metaApi } from '@/api/meta'

export const useModelStore = defineStore('model', () => {
  /** 模型名 -> 模型定义 的缓存 */
  const cache = new Map<string, ModelDef>()

  async function get(name: string): Promise<ModelDef> {
    const cached = cache.get(name)
    if (cached) {
      return cached
    }
    const model = await metaApi.model(name)
    cache.set(name, model)
    return model
  }

  /** 清掉一个模型的缓存；不传参数时清空全部 */
  function invalidate(name?: string): void {
    if (name === undefined) {
      cache.clear()
    } else {
      cache.delete(name)
    }
  }

  return { get, invalidate }
})
