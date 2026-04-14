import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'
import tailwindcss from "tailwindcss";

createApp(App).use(router).mount('#app')
