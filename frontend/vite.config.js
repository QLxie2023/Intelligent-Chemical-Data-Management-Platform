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
        
        // 可选：如果你在前端请求时不想要 '/api' 这个前缀，而后端路由也不需要它，可以进行路径重写
        // pathRewrite: {
        //   '^/api': '' // 重写后，请求 /api/users 会变成 /users 发给后端
        // },
      }
    }
  }
})