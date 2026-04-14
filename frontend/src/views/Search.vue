<template>
  <div class="min-h-screen flex bg-gray-100">
    <aside class="z-10 w-64 bg-white shadow-lg flex flex-col">
      <!-- Logo + Title -->
      <div class="flex items-center gap-3 p-6">
        <img
          src="../assets/logo.png"
          alt="Chem+ Logo"
          class="w-10 h-10 rounded-full object-cover shadow"
        />

        <span class="text-2xl font-bold text-blue-700">
          Chem+
        </span>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 space-y-2 px-4 text-gray-700">
        <router-link class="nav-item" to="/" exact>dashboard</router-link>
        <router-link class="nav-item" to="/user">user management</router-link>
        <!-- <router-link class="nav-item" to="/search">intelligent retrieval</router-link> -->
        <router-link class="nav-item" to="/insight">data insight</router-link>
        <router-link class="nav-item" to="/normal_user">personal data</router-link>
      </nav>

      <!-- Logout -->
      <button
        @click="logout"
        class="m-4 bg-red-500 text-white py-2 rounded-lg hover:bg-red-600 transition"
      >
        log out safely
      </button>
    </aside>

    <!-- main -->
    <main class="flex-1 p-8 overflow-y-auto">

      <!-- Title -->
      <h2 class="text-3xl font-bold text-gray-900 mb-6">intelligent retrieval</h2>

      <!-- Search -->
      <div class="bg-white p-4 rounded-xl shadow-lg flex items-center mb-6">
        <i class="text-gray-400 mr-3">🔍</i>
        <input
          v-model="keyword"
          type="text"
          placeholder="Enter keywords, for example: Aspirin synthesis"
          class="flex-1 text-lg outline-none bg-transparent"
        >
        <button
          @click="performSearch"
          class="ml-3 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          Search
        </button>
      </div>

      <!-- Search list -->
      <div v-if="loading" class="text-gray-500 text-lg">Searching...</div>

      <div v-if="results.length === 0 && !loading" class="text-gray-500">
        No results
      </div>

      <div v-for="item in results" :key="item.id"
        class="mt-4 bg-white p-5 rounded-xl shadow-md border-l-4 border-blue-500 hover:shadow-lg transition cursor-pointer"
        @click="openProject(item.id)"
      >
        <h3 class="text-xl font-semibold text-blue-800">{{ item.name }}</h3>
        <p class="text-sm text-gray-600 mt-2">{{ item.description }}</p>

        <div class="mt-3 text-sm text-gray-500">
          visibility:{{ item.visibility }} ｜ create date:{{ item.createdAt }}
        </div>
      </div>

    </main>

  </div>
</template>



<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import request from "../utils/request"; 

const router = useRouter();

const keyword = ref("");
const results = ref([]);
const loading = ref(false);

const performSearch = async () => {
  if (!keyword.value.trim()) {
    alert("Please enter the search keyword!");
    return;
  }

  loading.value = true;
  results.value = [];

  try {
    const token = localStorage.getItem("token");

    const res = await request.get("/projects/search", {
      params: { keyword: keyword.value },
      headers: {
        Authorization: `Bearer ${token}`
      }
    });

    if (res.code === 200) {
      results.value = res.data || [];
    } else {
      alert(res.message || "Search failed");
    }

  } catch (err) {
    alert("Search Error:" + err.message);
  }

  loading.value = false;
};

const openProject = (id) => {
  router.push(`/projects/${id}`);
};

//logout
const logout = () => {
  localStorage.clear();
  router.push("/login");
};
</script>



<style scoped>
@reference "../assets/main.css";

/* Css */
.nav-item {
  @apply block px-4 py-3 rounded-lg text-gray-700 font-medium
         hover:bg-blue-50 hover:text-blue-600 transition-all duration-200;
}

.router-link-exact-active {
  @apply bg-blue-600 text-white shadow-sm;
}

.router-link-active {
  @apply bg-blue-500 text-white;
}
</style>
