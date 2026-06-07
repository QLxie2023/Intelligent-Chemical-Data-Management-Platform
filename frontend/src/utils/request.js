import axios from "axios";

// Change baseURL to a relative path and use the proxy prefix defined in vite.config.js
const service = axios.create({
  baseURL: "/api/v1", // Note: no longer includes the full 'http://...'
  timeout: 100000
});

// Request interceptor that attaches the token
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

// Response interceptor
service.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    return Promise.reject(error.response?.data || error);
  }
);

export default service;