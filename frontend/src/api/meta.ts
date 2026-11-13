import type { ImportResult, MenuNode, MetaBundle, MetaBundleInput } from '@/types/api'
import type { ModelDef, ModelInput, ModuleDef } from '@/types/meta'

import { request } from './request'

/** 元模型接口，见 docs/contract/api.md 的“元模型接口” */
export const metaApi = {
  menus: () => request<MenuNode[]>({ url: '/meta/menus' }),

  modules: () => request<ModuleDef[]>({ url: '/meta/modules' }),
  saveModule: (module: ModuleDef) =>
    request<ModuleDef>({ url: `/meta/modules/${module.name}`, method: 'put', data: module }),
  removeModule: (name: string) => request<null>({ url: `/meta/modules/${name}`, method: 'delete' }),

  models: () => request<ModelDef[]>({ url: '/meta/models' }),
  model: (name: string) => request<ModelDef>({ url: `/meta/models/${name}` }),
  createModel: (model: ModelInput) =>
    request<ModelDef>({ url: '/meta/models', method: 'post', data: model }),
  updateModel: (name: string, model: ModelInput) =>
    request<ModelDef>({ url: `/meta/models/${name}`, method: 'put', data: model }),
  removeModel: (name: string) => request<null>({ url: `/meta/models/${name}`, method: 'delete' }),

  exportAll: () => request<MetaBundle>({ url: '/meta/export' }),
  importBundle: (bundle: MetaBundleInput) =>
    request<ImportResult>({ url: '/meta/import', method: 'post', data: bundle }),
}
