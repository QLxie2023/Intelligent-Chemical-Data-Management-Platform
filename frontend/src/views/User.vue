<template>
  <div class="min-h-screen flex bg-gray-100">

    <aside class="z-10 w-64 bg-white shadow-lg flex flex-col">
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

      <nav class="flex-1 space-y-2 px-4 text-gray-700">
        <router-link class="nav-item" to="/" exact>dashboard</router-link>
        <router-link class="nav-item" to="/user">user management</router-link>
        <router-link class="nav-item" to="/insight">data insight</router-link>
        <router-link class="nav-item" to="/normal_user">personal data</router-link>
      </nav>

      <button
        @click="logout"
        class="m-4 bg-red-500 text-white py-2 rounded-lg hover:bg-red-600 transition"
      >
        log out safely
      </button>
    </aside>

    <main class="flex-1 p-8">

      <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold">User Management</h1>

        <input
          type="text"
          v-model="searchKeyword"
          placeholder="Search Users (Username / Email)"
          class="px-4 py-2 border rounded-lg w-64 shadow-sm focus:ring-2 focus:ring-blue-500 outline-none"
        >
      </div>

      <div class="grid grid-cols-3 gap-6">

        <div class="col-span-2 bg-white rounded-xl shadow p-6">
          <h2 class="text-xl font-bold text-gray-700 mb-4">System User List</h2>

          <table class="w-full table-auto text-left">
            <thead>
              <tr class="text-gray-600 border-b">
                <th class="py-2">ID</th>
                <th class="py-2">Username</th>
                <th class="py-2">Email</th>
                <th class="py-2">Role</th>
                <th class="py-2">Status</th>
              </tr>
            </thead>

            <tbody>
              <tr
                v-for="user in filteredUsers"
                :key="user.id"
                class="hover:bg-gray-50 cursor-pointer border-b last:border-0"
                @click="selectUser(user)"
              >
                <td class="py-3">{{ user.id }}</td>
                <td class="py-3 font-medium text-blue-600">{{ user.username }}</td>
                <td class="py-3">{{ user.email }}</td>
                <td class="py-3">{{ user.role }}</td>
                <td class="py-3">
                  <span
                    class="px-2 py-1 rounded text-sm text-white"
                    :class="user.active ? 'bg-green-500' : 'bg-gray-400'"
                  >
                    {{ user.active ? 'Active' : 'Disabled' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
          
          <div v-if="filteredUsers.length === 0" class="text-center py-10 text-gray-400">
            No users found matching your search.
          </div>
        </div>

        <div class="col-span-1 bg-white rounded-xl shadow p-6" v-if="selectedUser">
          <h2 class="text-xl font-bold text-gray-700 mb-4">User Details</h2>

          <div class="space-y-3">
            <p class="text-gray-600"><strong>ID:</strong> {{ selectedUser.id }}</p>
            <p class="text-gray-600"><strong>Username:</strong> {{ selectedUser.username }}</p>
            <p class="text-gray-600"><strong>Email:</strong> {{ selectedUser.email }}</p>
            <p class="text-gray-600"><strong>Role:</strong> {{ selectedUser.role }}</p>
            
            <p class="text-gray-600">
              <strong>Status:</strong>
              <span
                class="ml-2 px-2 py-1 rounded text-sm text-white"
                :class="selectedUser.active ? 'bg-green-500' : 'bg-gray-400'"
              >
                {{ selectedUser.active ? 'Active' : 'Disabled' }}
              </span>
            </p>

            <p class="text-gray-600"><strong>Created At:</strong> {{ selectedUser.createdAt }}</p>
          </div>

          <hr class="my-6 border-gray-100">

          <h3 class="font-semibold text-gray-700 mb-3 text-lg">Activity Statistics</h3>
          <div class="space-y-2">
            <div class="flex justify-between text-gray-600">
              <span>Total Projects:</span>
              <span class="font-medium">{{ selectedUser.projectCount }}</span>
            </div>
            <div class="flex justify-between text-gray-600">
              <span>Files Uploaded:</span>
              <span class="font-medium">{{ selectedUser.fileCount }}</span>
            </div>
            <div class="flex justify-between text-gray-600">
              <span>Images Uploaded:</span>
              <span class="font-medium">{{ selectedUser.imageCount }}</span>
            </div>
          </div>
        </div>

        <div v-else class="col-span-1 text-gray-400 flex flex-col items-center justify-center bg-gray-50 border-2 border-dashed border-gray-200 rounded-xl p-6">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-12 w-12 mb-2 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
          </svg>
          <p>Select a user from the list to view details</p>
        </div>

      </div>

    </main>

  </div>
</template>


<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

// Mock User Data
const users = ref([
  {
    id: 1,
    username: "alice",
    email: "alice@example.com",
    role: "Researcher",
    active: true,
    projectCount: 5,
    fileCount: 12,
    imageCount: 8,
    createdAt: "2025-01-10"
  },
  {
    id: 2,
    username: "bob",
    email: "bob@example.com",
    role: "Admin",
    active: true,
    projectCount: 12,
    fileCount: 30,
    imageCount: 15,
    createdAt: "2024-11-22"
  },
  {
    id: 3,
    username: "carol",
    email: "carol@example.com",
    role: "Researcher",
    active: false,
    projectCount: 2,
    fileCount: 4,
    imageCount: 1,
    createdAt: "2023-07-01"
  },
]);

// Search Keyword
const searchKeyword = ref("");

// Currently Selected User
const selectedUser = ref(null);

// Select User Function
const selectUser = (user) => {
  selectedUser.value = user;
};

// Search Filter Logic
const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value;

  const key = searchKeyword.value.toLowerCase();

  return users.value.filter(u =>
    u.username.toLowerCase().includes(key) ||
    u.email.toLowerCase().includes(key)
  );
});

// Logout Function
const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  router.push("/login");
};
</script>


<style scoped>
@reference "../assets/main.css";

.nav-item {
  @apply block px-4 py-2 rounded-lg hover:bg-blue-100 hover:text-blue-600 transition;
}

.router-link-exact-active {
  @apply bg-blue-600 text-white shadow-sm;
}

.router-link-active {
  @apply bg-blue-500 text-white;
}

/* Custom scrollbar for side nav if needed */
nav::-webkit-scrollbar {
  width: 4px;
}
nav::-webkit-scrollbar-thumb {
  @apply bg-gray-200 rounded;
}
</style>