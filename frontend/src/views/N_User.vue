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
        <!-- <router-link class="nav-item" to="/search">intelligent retrieval</router-link> -->
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

    <main class="flex-1 p-8 overflow-y-auto">

      <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold">Personal Data</h1>
      </div>

      <div class="grid grid-cols-3 gap-6">

        <div class="col-span-1 bg-white rounded-xl shadow p-6">
          <h2 class="text-xl font-bold text-gray-700 mb-4">User Profile</h2>

          <div class="space-y-3">
            <label class="block">
              <span class="text-gray-600">Username</span>
              <input v-model="profile.username"
                class="w-full px-3 py-2 border rounded-lg mt-1"/>
            </label>

            <label class="block">
              <span class="text-gray-600">Email</span>
              <input v-model="profile.email"
                class="w-full px-3 py-2 border rounded-lg mt-1"/>
            </label>

            <p class="text-gray-600">
              <strong>Role:</strong> {{ profile.role }}
            </p>

            <p class="text-gray-600">
              <strong>Created At:</strong> {{ profile.createdAt }}
            </p>

            <button
              class="mt-4 w-full bg-blue-600 py-2 text-white rounded-lg hover:bg-blue-700"
            >
              Save Changes
            </button>
          </div>
        </div>

        <div class="col-span-1 bg-white rounded-xl shadow p-6">
          <h2 class="text-xl font-bold text-gray-700 mb-4">Change Password</h2>

          <div class="space-y-3">
            <label class="block">
              <span class="text-gray-600">Current Password</span>
              <input type="password" v-model="password.old"
                class="w-full px-3 py-2 border rounded-lg mt-1"/>
            </label>

            <label class="block">
              <span class="text-gray-600">New Password</span>
              <input type="password" v-model="password.new1"
                class="w-full px-3 py-2 border rounded-lg mt-1"/>
            </label>

            <label class="block">
              <span class="text-gray-600">Confirm New Password</span>
              <input type="password" v-model="password.new2"
                class="w-full px-3 py-2 border rounded-lg mt-1"/>
            </label>

            <p class="text-red-600" v-if="passwordError">{{ passwordError }}</p>

            <button
              @click="changePassword"
              class="mt-3 w-full bg-blue-600 py-2 text-white rounded-lg hover:bg-blue-700"
            >
              Update Password
            </button>
          </div>
        </div>

        <div class="col-span-1 bg-white rounded-xl shadow p-6">
          <h2 class="text-xl font-bold text-gray-700 mb-4">My Upload History</h2>

          <div class="space-y-3 max-h-[500px] overflow-y-auto">

            <div
              v-for="item in uploadHistory"
              :key="item.id"
              class="flex items-center justify-between p-3 border rounded-lg hover:bg-gray-50 transition"
            >
              <div>
                <p class="font-semibold text-gray-800">{{ item.fileName }}</p>
                <p class="text-gray-500 text-sm">
                  {{ item.uploadTimestamp }}
                </p>
              </div>

              <span class="px-2 py-1 text-white rounded text-sm"
                :class="item.type === 'image' ? 'bg-green-500' : 'bg-blue-600'"
              >
                {{ item.type }}
              </span>
            </div>

          </div>

        </div>

      </div>

    </main>

  </div>
</template>


<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

/* ------------------------ Personal Profile (Mock Data) ------------------------ */
const profile = ref({
  username: "fisherr",
  email: "fisherr@example.com",
  role: "Researcher",
  createdAt: "2025-01-12"
});

/* ------------------------ Password Modification ------------------------ */
const password = ref({
  old: "",
  new1: "",
  new2: ""
});

const passwordError = ref("");

const changePassword = () => {
  passwordError.value = "";

  if (!password.value.old || !password.value.new1 || !password.value.new2) {
    passwordError.value = "All fields are required";
    return;
  }

  if (password.value.new1 !== password.value.new2) {
    passwordError.value = "The new passwords do not match";
    return;
  }

  if (password.value.old === password.value.new1) {
    passwordError.value = "The new password cannot be the same as the old password";
    return;
  }

  alert("Password successfully changed (Simulated)");
};

/* ------------------------ Upload History (Mock Data) ------------------------ */
const uploadHistory = ref([
  {
    id: 101,
    type: "file",
    fileName: "experiment_report_final.pdf",
    uploadTimestamp: "2025-11-26 10:30"
  },
  {
    id: 102,
    type: "image",
    fileName: "reaction_setup.jpg",
    uploadTimestamp: "2025-11-26 11:05"
  }
]);

/* ------------------------ Logout ------------------------ */
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
  @apply bg-blue-600 text-white;
}
</style>