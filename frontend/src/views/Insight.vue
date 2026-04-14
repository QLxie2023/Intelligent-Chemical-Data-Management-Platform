<template>
  <div class="h-screen flex bg-gray-100 overflow-hidden">
    
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

    <main class="flex-1 flex flex-col h-full p-8 overflow-hidden">
      
      <div class="flex justify-between items-center mb-6 shrink-0">
        <div>
          <h1 class="text-3xl font-bold text-gray-800">Project Data Insights</h1>
          <p class="text-gray-500 text-sm mt-1">Manage and visualize your chemical research data</p>
        </div>

        <button
          @click="showCreateModal = true; resetFormAndFiles();"
          class="inline-flex items-center px-5 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition shadow-md active:transform active:scale-95"
        >
          <span class="mr-2 text-lg">+</span> Create new project
        </button>
      </div>

      <div class="flex-1 flex flex-col bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
        <div class="px-6 py-4 border-b border-gray-100 bg-gray-50/50 flex justify-between items-center">
          <h2 class="text-lg font-bold text-gray-700">Data Association Knowledge Graph</h2>
          <div class="flex gap-2">
            <span class="w-3 h-3 rounded-full bg-green-400 animate-pulse"></span>
            <span class="text-xs text-gray-400 font-medium uppercase tracking-wider">Live Visualization</span>
          </div>
        </div>

        <div class="flex-1 relative w-full bg-gray-50">
          <iframe
            src="http://localhost:8000"
            class="absolute inset-0 w-full h-full border-0"
            title="Knowledge Graph Visualization"
          ></iframe>
        </div>
      </div>
    </main>

    <div
      v-if="showCreateModal"
      class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4"
    >
      <div class="bg-white w-full max-w-lg rounded-xl shadow-2xl p-6 overflow-y-auto max-h-[90vh]">
        <h2 class="text-xl font-bold mb-5 text-gray-800 border-b pb-3">Create New Project</h2>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Project Name</label>
            <input v-model="newProject.name" type="text" placeholder="e.g. Synthesis of Aspirin"
              class="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
            <textarea v-model="newProject.description" placeholder="Describe project goals..."
              class="w-full px-4 py-2 border rounded-lg h-24 focus:ring-2 focus:ring-blue-500 outline-none"></textarea>
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Visibility</label>
            <select v-model="newProject.visibility" class="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none">
              <option value="PUBLIC">Public (Visible to everyone)</option>
              <option value="PRIVATE">Private (Only you)</option>
            </select>
          </div>

          <div class="p-4 bg-blue-50/50 border border-blue-100 rounded-lg">
            <p class="font-semibold text-sm text-blue-800 mb-2">Experimental Files (.pdf / .docx)</p>
            <input type="file" ref="fileInput" @change="selectFile" class="text-sm block w-full file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-600 file:text-white hover:file:bg-blue-700" />
            <p v-if="fileUploadMsg" :class="fileUploadMsg.includes('fail') ? 'text-red-600' : 'text-green-600'" class="text-xs mt-2">{{ fileUploadMsg }}</p>
          </div>

          <div class="p-4 bg-gray-50 border border-gray-200 rounded-lg">
            <p class="font-semibold text-sm text-gray-800 mb-2">Experimental Images (.jpg / .png)</p>
            <input type="file" ref="imageInput" @change="selectImage" class="text-sm block w-full file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-gray-600 file:text-white hover:file:bg-gray-700" />
            <p v-if="imageUploadMsg" :class="imageUploadMsg.includes('fail') ? 'text-red-600' : 'text-green-600'" class="text-xs mt-2">{{ imageUploadMsg }}</p>
          </div>
        </div>

        <p v-if="errorMsg" class="text-red-600 text-sm mt-4 bg-red-50 p-2 rounded">{{ errorMsg }}</p>

        <div class="flex justify-end space-x-3 mt-8">
          <button
            @click="showCreateModal = false"
            class="px-4 py-2 bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200 transition"
            :disabled="isCreating"
          >Cancel</button>

          <button
            @click="createProject"
            class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-blue-400 shadow-md transition"
            :disabled="isCreating"
          >
            {{ isCreating ? 'Processing...' : 'Create Project' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
@reference "../assets/main.css";

/* 导航项激活状态和悬停效果 */
.nav-item {
  @apply block px-4 py-2.5 rounded-lg hover:bg-blue-50 hover:text-blue-600 transition-all duration-200 font-medium;
}

.router-link-exact-active {
  @apply bg-blue-600 text-white shadow-md hover:bg-blue-700 hover:text-white;
}

/* 优化滚动条样式 */
::-webkit-scrollbar {
  width: 5px;
}
::-webkit-scrollbar-track {
  @apply bg-transparent;
}
::-webkit-scrollbar-thumb {
  @apply bg-gray-300 rounded-full;
}
::-webkit-scrollbar-thumb:hover {
  @apply bg-gray-400;
}
</style>

<script setup>
import { ref } from "vue";
import request from "../utils/request";
import { useRouter } from "vue-router";

const router = useRouter();

const showCreateModal = ref(false);
const isCreating = ref(false);
const newProject = ref({
  name: "",
  description: "",
  visibility: "PRIVATE",
});

const errorMsg = ref("");
const selectedFile = ref(null);
const selectedImage = ref(null);
const fileUploadMsg = ref("");
const imageUploadMsg = ref("");

const fileInput = ref(null);
const imageInput = ref(null);

const resetFormAndFiles = () => {
  newProject.value = { name: "", description: "", visibility: "PRIVATE" };
  selectedFile.value = null;
  selectedImage.value = null;
  fileUploadMsg.value = "";
  imageUploadMsg.value = "";
  if (fileInput.value) fileInput.value.value = "";
  if (imageInput.value) imageInput.value.value = "";
};

const selectFile = (e) => {
  selectedFile.value = e.target.files[0];
  fileUploadMsg.value = selectedFile.value ? `Ready: ${selectedFile.value.name}` : "";
};

const selectImage = (e) => {
  selectedImage.value = e.target.files[0];
  imageUploadMsg.value = selectedImage.value ? `Ready: ${selectedImage.value.name}` : "";
};

const uploadFile = async (projectId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  try {
    const res = await request.post(`/projects/${projectId}/files`, formData);
    fileUploadMsg.value = res.code === 200 ? "File uploaded." : `Fail: ${res.message}`;
  } catch {
    fileUploadMsg.value = "Network error on file.";
  }
};

const uploadImage = async (projectId, img) => {
  const formData = new FormData();
  formData.append("image", img);
  try {
    const res = await request.post(`/projects/${projectId}/images`, formData);
    imageUploadMsg.value = res.code === 200 ? "Image uploaded." : `Fail: ${res.message}`;
  } catch {
    imageUploadMsg.value = "Network error on image.";
  }
};

const createProject = async () => {
  errorMsg.value = "";
  if (!newProject.value.name || !newProject.value.description) {
    errorMsg.value = "Please complete project details.";
    return;
  }
  isCreating.value = true;
  try {
    const res = await request.post("/projects", newProject.value);
    if (res.code === 200) {
      const id = res.data.projectId;
      if (selectedFile.value) await uploadFile(id, selectedFile.value);
      if (selectedImage.value) await uploadImage(id, selectedImage.value);
      showCreateModal.value = false;
      alert("Project created successfully!");
    } else {
      errorMsg.value = res.message;
    }
  } catch {
    errorMsg.value = "Creation failed. Check connection or login status.";
  }
  isCreating.value = false;
  resetFormAndFiles();
};

const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  router.push("/login");
};
</script>