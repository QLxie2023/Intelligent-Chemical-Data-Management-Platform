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
        <!-- <router-link class="nav-item" to="/search">intelligent retrieval</router-link> -->
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
            <p class="text-red-600" v-if="profileError">{{ profileError }}</p>
            
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
             
            </p>

            <button
              @click="openSaveConfirm"
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
              @click="openPasswordConfirm"
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

    <!-- Confirm Dialog -->
    <div
      v-if="showConfirmDialog"
      class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/35 px-4 backdrop-blur-[2px]"
      @click.self="cancelConfirm"
    >
      <section class="w-full max-w-md overflow-hidden rounded-lg border border-gray-200 bg-white shadow-xl">
        <header class="flex items-center gap-3 border-b border-gray-200 px-5 py-4">
          <span class="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-blue-50 text-blue-600">
            <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
            </svg>
          </span>
          <div class="min-w-0">
            <h2 class="text-lg font-semibold leading-6 text-gray-900">{{ confirmDialogTitle }}</h2>
            <p class="mt-0.5 text-sm leading-5 text-gray-500">{{ confirmDialogMessage }}</p>
          </div>
        </header>

        <div class="space-y-4 px-5 py-5">
          <p class="text-sm text-gray-600">{{ confirmDialogContent }}</p>
        </div>

        <footer class="flex justify-end gap-3 border-t border-gray-200 bg-gray-50 px-5 py-4">
          <button
            type="button"
            @click="cancelConfirm"
            class="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition"
          >
            Cancel
          </button>
          <button
            type="button"
            @click="confirmAction"
            class="rounded-md border border-blue-600 bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 transition"
          >
            Confirm
          </button>
        </footer>
      </section>
    </div>

  </div>
</template>


<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import request from "../utils/request";

const router = useRouter();

/* ------------------------ Personal Profile ------------------------ */
const profile = ref({
  username: "",
  email: "",
  role: "",
  createdAt: ""
});

/* ------------------------ Password Modification ------------------------ */
const password = ref({
  old: "",
  new1: "",
  new2: ""
});

const passwordError = ref("");
const profileError = ref("");

/* ------------------------ Confirm Dialog ------------------------ */
const showConfirmDialog = ref(false);
const confirmDialogTitle = ref("");
const confirmDialogMessage = ref("");
const confirmDialogContent = ref("");
const pendingAction = ref(null); // 'save' or 'password'

/* ------------------------ Upload History ------------------------ */
const uploadHistory = ref([]);

/* ------------------------ Get User Info ------------------------ */
const getUserInfo = async () => {
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
      throw new Error("Failed to fetch user info");
    }
    
    const result = await response.json();
    if (result.code === 200) {
      profile.value = {
        username: result.data.username,
        email: result.data.email || "",
        role: result.data.role,
        createdAt: result.data.createdAt || "" // Get from API
      };
    } else {
      profileError.value = result.message || "Failed to fetch user info";
    }
  } catch (error) {
    console.error("Failed to fetch user info:", error);
    profileError.value = "Failed to fetch user info";
  }
};

/* ------------------------ Save Profile ------------------------ */
const saveProfile = async () => {
  profileError.value = "";
  
  if (!profile.value.email) {
    profileError.value = "Email is required";
    return;
  }
  
  try {
    const token = localStorage.getItem("token");
    const response = await fetch("/api/v1/users/profile", {
      method: "PUT",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        email: profile.value.email,
        displayName: profile.value.username
      })
    });
    
    const result = await response.json();
    if (result.code === 200) {
      console.log("Profile saved successfully");
      // Refresh user info
      await getUserInfo();
      cancelConfirm();
    } else {
      profileError.value = result.message || "Failed to save profile";
    }
  } catch (error) {
    console.error("Failed to save profile:", error);
    profileError.value = "Failed to save profile";
  }
};

/* ------------------------ Confirm Dialog Functions ------------------------ */
const openSaveConfirm = () => {
  if (!profile.value.email) {
    profileError.value = "Email is required";
    return;
  }
  confirmDialogTitle.value = "Save Profile Changes?";
  confirmDialogMessage.value = "Are you sure you want to save these changes?";
  confirmDialogContent.value = `Your email will be updated to: ${profile.value.email}`;
  pendingAction.value = "save";
  showConfirmDialog.value = true;
};

const openPasswordConfirm = () => {
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
  confirmDialogTitle.value = "Update Password?";
  confirmDialogMessage.value = "Are you sure you want to update your password?";
  confirmDialogContent.value = "Your password will be updated to the new value you entered.";
  pendingAction.value = "password";
  showConfirmDialog.value = true;
};

const cancelConfirm = () => {
  showConfirmDialog.value = false;
  pendingAction.value = null;
};

const confirmAction = async () => {
  if (pendingAction.value === "save") {
    await saveProfile();
  } else if (pendingAction.value === "password") {
    await changePassword();
  }
};

/* ------------------------ Get Upload History ------------------------ */
const getUploadHistory = async () => {
  try {
    const token = localStorage.getItem("token");
    const response = await fetch("/api/v1/users/upload-history", {
      method: "GET",
      headers: {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json"
      }
    });
    
    if (!response.ok) {
      throw new Error("Failed to fetch upload history");
    }
    
    const result = await response.json();
    if (result.code === 200) {
      uploadHistory.value = result.data;
    } else {
      console.error("Failed to fetch upload history:", result.message);
    }
  } catch (error) {
    console.error("Failed to fetch upload history:", error);
  }
};

/* ------------------------ Change Password ------------------------ */
const changePassword = async () => {
  passwordError.value = "";

  try {
    const user = JSON.parse(localStorage.getItem("user"));
    if (!user || !user.username) {
      router.push("/login");
      return;
    }

    const response = await request.post(`/auth/change-password?username=${user.username}`, {
      oldPassword: password.value.old,
      newPassword: password.value.new1
    });

    if (response.code === 200) {
      console.log("Password successfully changed");
      password.value = {
        old: "",
        new1: "",
        new2: ""
      };
      cancelConfirm();
    } else {
      passwordError.value = response.message || "Failed to change password";
    }
  } catch (error) {
    console.error("Failed to change password:", error);
    passwordError.value = "Failed to change password";
  }
};

/* ------------------------ Logout ------------------------ */
const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  router.push("/login");
};

/* ------------------------ Mounted ------------------------ */
onMounted(() => {
  getUserInfo();
  getUploadHistory();
});
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