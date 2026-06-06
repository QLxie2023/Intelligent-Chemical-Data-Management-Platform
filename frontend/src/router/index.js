// src/router/index.js
import { createRouter, createWebHistory } from "vue-router";
import Dashboard from "../views/Dashboard.vue";
import ProjectManagement from "../views/Home.vue";
import Login from "../views/Login.vue";
import Register from "../views/Register.vue";
import Search from "../views/Search.vue";
import User from "../views/User.vue";
import ProjectDetail from '../views/ProjectDetail.vue';
import N_User from "../views/N_User.vue";

const routes = [
  { path: "/", redirect: "/project-management" },
  { path: "/login", component: Login },
  { path: "/dashboard", name: "Dashboard", component: Dashboard, meta: { requiresAuth: true } },
  { path: "/project-management", name: "ProjectManagement", component: ProjectManagement, meta: { requiresAuth: true } },
  { path: "/register", component: Register, meta: { requiresAuth: true } },
  { path: "/search", component: Search, meta: { requiresAuth: true } },
  { path: "/user", component: User, meta: { requiresAuth: true } },
  { path:"/normal_user", component: N_User, meta: { requiresAuth: true } },
  {
    path: '/projects/:id', // 动态匹配 /projects/1, /projects/2 等
    name: 'ProjectDetail',
    component: ProjectDetail,
    meta: { requiresAuth: true }
    // props: true // 允许将路由参数作为 props 传递给组件 (可选)
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");

  if (to.meta.requiresAuth && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;