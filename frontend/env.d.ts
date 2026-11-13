/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 'true' 时所有接口走前端内存 mock，不请求后端 */
  readonly VITE_USE_MOCK: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
