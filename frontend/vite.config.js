/**
 * Vite 配置文件，负责定义前端构建与开发服务配置。
 */
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 现代管理端浏览器均支持 WOFF2；移除 Fontsource 的 WOFF 兼容副本，避免产物重复携带整套中文字库。
const fontsourceWoff2Only = () => ({
  name: 'fontsource-woff2-only',
  enforce: 'pre',
  transform(code, id) {
    if (!id.replace(/\\/g, '/').includes('/@fontsource/') || !id.endsWith('.css')) return null
    return code.replace(/,\s*url\([^)]*\.woff\)\s*format\(['"]woff['"]\)/g, '')
  }
})

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    fontsourceWoff2Only(),
    vue(),
    AutoImport({
      dts: false,
      resolvers: [ElementPlusResolver()]
    }),
    Components({
      dts: false,
      dirs: ['components'],
      resolvers: [ElementPlusResolver({ importStyle: 'css' })]
    })
  ],
  build: {
    rolldownOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (normalizedId.includes('/node_modules/zrender/')) return 'zrender'
          if (normalizedId.includes('/node_modules/echarts/lib/chart/')) return 'echarts-charts'
          if (normalizedId.includes('/node_modules/echarts/lib/component/')) return 'echarts-components'
          if (normalizedId.includes('/node_modules/echarts/')) return 'echarts-core'
          if (normalizedId.includes('/node_modules/element-plus/es/components/table/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/tree/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/pagination/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/virtual-list/')) return 'element-data'
          if (normalizedId.includes('/node_modules/element-plus/es/components/dialog/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/drawer/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/message') ||
            normalizedId.includes('/node_modules/element-plus/es/components/notification/') ||
            normalizedId.includes('/node_modules/element-plus/es/components/pop')) return 'element-overlay'
          if (normalizedId.includes('/node_modules/element-plus/')) return 'element-ui'
          if (normalizedId.includes('/node_modules/vue/') ||
            normalizedId.includes('/node_modules/vue-router/') ||
            normalizedId.includes('/node_modules/vuex/')) return 'vue'
          return undefined
        }
      }
    }
  },
  test: {
    environment: 'jsdom',
    setupFiles: './tests/setup.js',
    clearMocks: true,
    exclude: ['tests/e2e/**', 'node_modules/**', 'dist/**'],
    server: {
      deps: {
        inline: [/element-plus/]
      }
    }
  },
  server: {
    host: 'localhost',
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        // 处理rewrite
        rewrite: (path) => path
      }
    }
  }
})
