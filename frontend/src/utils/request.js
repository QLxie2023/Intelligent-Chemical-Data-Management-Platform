import axios from "axios";

// 修改：将 baseURL 改为相对路径，使用 vite.config.js 中定义的代理前缀
const service = axios.create({
  baseURL: "/api/v1", // 注意：不再包含完整的 'http://...'
  timeout: 100000
});

// 请求拦截器（带上 token）
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    return Promise.reject(error.response?.data || error);
  }
);

export default service;