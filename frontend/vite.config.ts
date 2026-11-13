import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 前端代码里只写 /api/...，开发时由这里转发给后端
      '/api': {
        target: 'http://localhost:8200',
        changeOrigin: true,
      },
    },
  },
})
