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
        <router-link class="nav-item" to="/dashboard">dashboard</router-link>
        <router-link class="nav-item" to="/project-management" exact>project management</router-link>
        <router-link class="nav-item" to="/user">user management</router-link>
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
                <th class="py-2">Delete</th>
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
                  <button
                    v-if="isAdmin"
                    @click.stop="openDeleteConfirm(user)"
                    class="rounded-md border border-red-200 bg-red-50 px-3 py-1.5 text-xs font-medium text-red-700 hover:border-red-300 hover:bg-red-100 transition"
                    title="Delete user"
                  >
                    Delete
                  </button>
                  <span v-else class="text-gray-400 text-xs">No permission</span>
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

    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/35 px-4 backdrop-blur-[2px]"
      @click.self="cancelDelete"
    >
      <section class="w-full max-w-md overflow-hidden rounded-lg border border-gray-200 bg-white shadow-xl">
        <header class="flex items-center gap-3 border-b border-gray-200 px-5 py-4">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-red-50 text-red-600">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M8.75 1A2.75 2.75 0 006 3.75V4H3.5a.75.75 0 000 1.5h.31l.72 10.12A2.75 2.75 0 007.27 18h5.46a2.75 2.75 0 002.74-2.38l.72-10.12h.31a.75.75 0 000-1.5H14v-.25A2.75 2.75 0 0011.25 1h-2.5zM7.5 4v-.25c0-.69.56-1.25 1.25-1.25h2.5c.69 0 1.25.56 1.25 1.25V4h-5zm1.25 4.25a.75.75 0 00-1.5 0v5.5a.75.75 0 001.5 0v-5.5zm4 0a.75.75 0 00-1.5 0v5.5a.75.75 0 001.5 0v-5.5z" clip-rule="evenodd" />
            </svg>
          </span>
          <div class="min-w-0">
            <h2 class="text-lg font-semibold leading-6 text-gray-900">Delete this user?</h2>
            <p class="mt-0.5 text-sm leading-5 text-gray-500">This action cannot be undone.</p>
          </div>
        </header>

        <div class="space-y-4 px-5 py-5">
          <p class="text-sm text-gray-600">
            The selected account
            <span v-if="pendingDeleteUser" class="font-medium text-gray-900">{{ pendingDeleteUser.username }}</span>
            will be removed from the system user list.
          </p>
          <p v-if="pendingDeleteUser" class="truncate text-sm text-gray-500">{{ pendingDeleteUser.email }}</p>
        </div>

        <footer class="flex justify-end gap-3 border-t border-gray-200 bg-gray-50 px-5 py-4">
          <button
            type="button"
            @click="cancelDelete"
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            @click="confirmDeleteUser"
            class="rounded-md border border-red-600 bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 transition"
          >
            Confirm Delete
          </button>
        </footer>
      </section>
    </div>

  </div>
</template>


<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

// User list data fetched from the backend
const users = ref([]);

// Current logged-in user information used for permission checks
const currentUser = ref(null);

// Check whether the current user is an administrator
const isAdmin = computed(() => {
  return currentUser.value && currentUser.value.role === "ROLE_ADMIN";
});

// Search Keyword
const searchKeyword = ref("");

// Currently Selected User
const selectedUser = ref(null);
const showDeleteConfirm = ref(false);
const pendingDeleteUser = ref(null);

// Fetch the user list from the backend
const fetchUsers = async () => {
  try {
    const token = localStorage.getItem("token");
    const response = await fetch("/api/v1/users/list", {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });
    
    if (!response.ok) {
      throw new Error("Failed to fetch users");
    }
    
    const result = await response.json();
    if (result.code === 200) {
      // Convert user data to frontend format
      users.value = result.data.map(user => ({
        id: user.userId,  // Use userId from backend response
        username: user.username,
        email: user.email,
        role: user.role.replace("ROLE_", ""), // Remove ROLE_ prefix
        displayName: user.displayName,
        projectCount: 0,
        fileCount: 0,
        imageCount: 0,
        createdAt: ""
      }));
    }
  } catch (error) {
    console.error("Error fetching users:", error);
  }
};

// Get current user information
const fetchCurrentUser = async () => {
  try {
    const token = localStorage.getItem("token");
    const response = await fetch("/api/v1/users/current", {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });
    
    if (!response.ok) {
      throw new Error("Failed to fetch current user");
    }
    
    const result = await response.json();
    if (result.code === 200) {
      currentUser.value = result.data;
    }
  } catch (error) {
    console.error("Error fetching current user:", error);
  }
};

// Select User Function
const selectUser = async (user) => {
  selectedUser.value = user;
  
  // Fetch user detail with statistics from backend
  try {
    const token = localStorage.getItem("token");
    const response = await fetch(`/api/v1/users/${user.id}`, {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });
    
    if (response.ok) {
      const result = await response.json();
      if (result.code === 200 && result.data) {
        selectedUser.value = {
          ...selectedUser.value,
          projectCount: result.data.projectCount || 0,
          fileCount: result.data.fileCount || 0,
          imageCount: result.data.imageCount || 0
        };
      }
    }
  } catch (error) {
    console.error("Error fetching user detail:", error);
  }
};

const openDeleteConfirm = (user) => {
  pendingDeleteUser.value = user;
  showDeleteConfirm.value = true;
};

const cancelDelete = () => {
  showDeleteConfirm.value = false;
  pendingDeleteUser.value = null;
};

// Delete User Function
const confirmDeleteUser = async () => {
  const user = pendingDeleteUser.value;
  if (!user || !user.id) {
    console.error("Delete failed: Invalid user or user ID");
    cancelDelete();
    return;
  }

  // Check if trying to delete own account
  if (currentUser.value && currentUser.value.userId === user.id) {
    console.error("Delete failed: Cannot delete your own account");
    cancelDelete();
    return;
  }

  try {
    const token = localStorage.getItem("token");
    const response = await fetch(`/api/v1/users/${user.id}`, {
      method: "DELETE",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      // Delete successful, update frontend list
      users.value = users.value.filter((item) => item.id !== user.id);
      
      if (selectedUser.value?.id === user.id) {
        selectedUser.value = null;
      }
      
      console.log("Delete successful");
    } else {
      console.error("Delete failed:", result.message || "Unknown error");
    }
  } catch (error) {
    console.error("Error deleting user:", error);
  }

  cancelDelete();
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

// Fetch the user list and current user information when the page loads
onMounted(() => {
  fetchUsers();
  fetchCurrentUser();
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