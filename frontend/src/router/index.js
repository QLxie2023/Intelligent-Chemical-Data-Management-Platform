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
  { path: "/register", component: Register},
  { path: "/search", component: Search, meta: { requiresAuth: true } },
  { path: "/user", component: User, meta: { requiresAuth: true } },
  { path:"/normal_user", component: N_User, meta: { requiresAuth: true } },
  {
    path: '/projects/:id', // Dynamically matches /projects/1, /projects/2, and similar paths
    name: 'ProjectDetail',
    component: ProjectDetail,
    meta: { requiresAuth: true }
    // props: true // Allow route params to be passed as props to the component (optional)
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// Global route guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");

  if (to.meta.requiresAuth && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;
