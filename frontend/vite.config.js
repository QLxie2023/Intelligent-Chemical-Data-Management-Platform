import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],

  // 重点：在这里添加 server 配置
  server: {
    // 代理配置
    proxy: {
      // 当请求以 '/api' 开头时，例如：axios.get('/api/users')
      '/api': {
        // 目标后端地址
        // 注意：这里我们使用你提供的 IP 和端口
        target: 'http://localhost:8080',
        
        // 允许跨域
        changeOrigin: true, 
        
        // 路径重写：将 /api 前缀重写为空字符串，这样请求 /api/v1/projects 会变成 /api/v1/projects 发给后端
        pathRewrite: {
          '^/api': '' // 重写后，请求 /api/v1/projects 会变成 /api/v1/projects 发给后端
        },
      }
    }
  }
})