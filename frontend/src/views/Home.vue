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


    <main class="flex-1 p-8">

      <div class="flex justify-between items-center mb-6">
        <h1 class="text-3xl font-bold">Project Overview</h1>

        <button
          @click="showCreateModal = true; resetFormAndFiles();"
          class="inline-block px-5 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          + Create new project
        </button>
      </div>

      <div class="grid grid-cols-3 gap-6 mb-8">
        <div class="card">
          <p class="text-gray-600">Number of experimental record</p>
          <p class="text-4xl font-bold mt-2">127</p>
        </div>

        <div class="card">
          <p class="text-gray-600">Number of pending files</p>
          <p class="text-4xl font-bold mt-2">2</p>
        </div>

        <div class="card">
          <p class="text-gray-600">Number of identified files</p>
          <p class="text-4xl font-bold mt-2">77</p>
        </div>
      </div>

      <section class="mb-8">
        <h2 class="text-xl font-semibold mb-4">My project list (total {{ projects.length }})</h2>
        
        <div class="bg-white rounded-xl shadow">
          <table class="w-full text-left table-auto">
            <tbody class="divide-y divide-gray-200">
                <tr 
                    v-for="project in projects" 
                    :key="project.projectId" 
                    class="hover:bg-gray-50 cursor-pointer" 
                    @click="goToProjectDetail(project.projectId)" 
                    >
                    <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">{{ project.projectId }}</td>
                    <td class="px-6 py-4 text-sm text-gray-700">{{ project.name }}</td>
                    <td class="px-6 py-4 text-sm text-gray-700">{{ project.description }}</td>
                    <td class="px-6 py-4 text-sm text-gray-700">{{ project.ownerUsername }}</td>
                </tr>
            </tbody>
          </table>
          
        </div>
        
        <p v-if="projectErrorMsg" class="mt-4 text-red-600">{{ projectErrorMsg }}</p>
      </section>
    </main>

    <div
      v-if="showCreateModal"
      class="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50"
    >
      <div class="bg-white w-full max-w-lg rounded-xl shadow-lg p-6">
        <h2 class="text-xl font-bold mb-4">Create new project</h2>
        
        <input
          v-model="newProject.name"
          type="text"
          placeholder="Project name"
          class="w-full px-4 py-2 mb-3 border rounded-lg"
        />

        <textarea
          v-model="newProject.description"
          placeholder="Project description"
          class="w-full px-4 py-2 mb-3 border rounded-lg h-24"
        ></textarea>

        <select
          v-model="newProject.visibility"
          class="w-full px-4 py-2 mb-4 border rounded-lg"
        >
          <option value="PUBLIC">Public</option>
          <option value="PRIVATE">Private</option>
        </select>
        
        <div class="mb-4 p-3 border rounded-lg">
            <p class="font-semibold mb-2">Option: Upload experimental files (.pdf / .docx)</p>
            <input type="file" ref="fileInput" @change="selectFile" class="mb-3" />
            <p v-if="fileUploadMsg" :class="fileUploadMsg.includes('fail') ? 'text-red-600' : 'text-green-600'" class="text-sm mt-1">{{ fileUploadMsg }}</p>
        </div>

        <div class="mb-4 p-3 border rounded-lg">
            <p class="font-semibold mb-2">Option: Upload experimental images (.jpg / .png)</p>
            <input type="file" ref="imageInput" @change="selectImage" class="mb-3" />
            <p v-if="imageUploadMsg" :class="imageUploadMsg.includes('fail') ? 'text-red-600' : 'text-green-600'" class="text-sm mt-1">{{ imageUploadMsg }}</p>
        </div>

        <p v-if="errorMsg" class="text-red-600 mb-2">{{ errorMsg }}</p>

        <div class="flex justify-end space-x-3">
          <button
            @click="showCreateModal = false"
            class="px-4 py-2 bg-gray-300 rounded-lg hover:bg-gray-400"
            :disabled="isCreating"
          >
            cancel
          </button>

          <button
            @click="createProject"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-blue-400"
            :disabled="isCreating"
          >
            {{ isCreating ? 'Creating and loading...' : 'create project' }}
          </button>
        </div>
      </div>
    </div>

    </div>
</template>

<style scoped>
@reference "../assets/main.css";

.nav-item {
  @apply block px-4 py-2 rounded-lg hover:bg-blue-100 hover:text-blue-600 transition;
}

.router-link-exact-active {
  @apply bg-blue-600 text-white;
}
</style>

<script setup>
import { ref, onMounted } from "vue";
import request from "../utils/request";
import { useRouter } from "vue-router";

import bgImage from "../assets/62052a6b54f453336bdf571ef993a9f4.jpg";

const router = useRouter();

const showCreateModal = ref(false);
const isCreating = ref(false);

const newProject = ref({
  name: "",
  description: "",
  visibility: "PRIVATE",
});

const errorMsg = ref(""); // Error message for project creation
const projectErrorMsg = ref(""); // Error message for obtaining the project list
const fileUploadMsg = ref(""); // File upload success/failure information
const imageUploadMsg = ref(""); // Image upload success/failure information

const projects = ref([]); // Project list data
const loadingProjects = ref(true); // Loading state

// --- choose files/images ---
const selectedFile = ref(null);
const selectedImage = ref(null);

// Used for resetting the file input box
const fileInput = ref(null);
const imageInput = ref(null);


const selectFile = (e) => {
  selectedFile.value = e.target.files[0];
  fileUploadMsg.value = selectedFile.value ? `已选择文件: ${selectedFile.value.name}` : '';
};

const selectImage = (e) => {
  selectedImage.value = e.target.files[0];
  imageUploadMsg.value = selectedImage.value ? `已选择图片: ${selectedImage.value.name}` : '';
};

// Reset the form and file selection
const resetFormAndFiles = () => {
    errorMsg.value = "";
    fileUploadMsg.value = "";
    imageUploadMsg.value = "";
    selectedFile.value = null;
    selectedImage.value = null;
    if (fileInput.value) fileInput.value.value = '';
    if (imageInput.value) imageInput.value.value = '';
};

// Obtain the list of projects
const fetchProjects = async () => {
  loadingProjects.value = true;
  projectErrorMsg.value = "";
  try {
    const res = await request.get("/projects"); 
    
    if (res.code === 200 && Array.isArray(res.data)) {
      projects.value = res.data;
    } else {
      projectErrorMsg.value = res.message || "Failed to obtain the project list";
    }
  } catch (err) {
    console.error("Failed to obtain the project list:", err);
    projectErrorMsg.value = "The project acquisition failed. Please check the network or the backend service.";
  } finally {
    loadingProjects.value = false;
  }
};


// Upload file (
const uploadFile = async (projectId, file) => {
  const formData = new FormData();
  formData.append("file", file); 

  try {
    const res = await request.post(
      `/projects/${projectId}/files`,
      formData,
    );

    if (res.code === 200) {
      fileUploadMsg.value = `Upload successful!ID: ${res.data.fileId}`;
    } else {
      fileUploadMsg.value = `Failed to upload:${res.message || 'Unkown error'}`;
    }
  } catch (err) {
    console.error("Failed to upload:", err);
    fileUploadMsg.value = "Failed to upload, please check network or back-end.";
  }
};

// Upload images 
const uploadImage = async (projectId, image) => {
  const formData = new FormData();
  formData.append("image", image);

  try {
    const res = await request.post(
      `/projects/${projectId}/images`,
      formData,
    );

    if (res.code === 200) {
      imageUploadMsg.value = `Upload image successful! ID: ${res.data.imageId}`;
    } else {
      imageUploadMsg.value = `Failed upload images:${res.message || 'Unknown error'}`;
    }
  } catch (err) {
    console.error("Failed upload images:", err);
    imageUploadMsg.value = "Failed to upload, please check network or back-end.";
  }
};


// Create new project 
const createProject = async () => {
  errorMsg.value = "";

  if (!newProject.value.name || !newProject.value.description) {
    errorMsg.value = "Please fill in all the project details.";
    return;
  }
  
  isCreating.value = true;
  
  try {
    // 1. create project
    const createRes = await request.post("/projects", newProject.value);

    if (createRes.code === 200) {
      const projectId = createRes.data.projectId;
      
      // 2. Upload file (if selected)
      if (selectedFile.value) {
          await uploadFile(projectId, selectedFile.value);
      }
      
      // 3. Upload images (if selected)
      if (selectedImage.value) {
          await uploadImage(projectId, selectedImage.value);
      }
      
      // etc.
      alert("The project creation and file upload tasks have been completed!");

      showCreateModal.value = false;
      fetchProjects(); // flash

    } else {
      errorMsg.value = createRes.message || "Creation failed";
    }

  } catch (err) {
    console.error("Project creation or upload failed:", err);
    errorMsg.value = "The operation failed. Please confirm that the CORS configuration at the backend has allowed the 'Authorization' header.";
  } finally {
    isCreating.value = false;
    resetFormAndFiles();
  }
};

// Project click-to-redirect
const goToProjectDetail = (projectId) => {
    router.push({ name: 'ProjectDetail', params: { id: projectId } });
};

// Other functions
const logout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  router.push("/login");
};

// Get project list
onMounted(() => {
  fetchProjects();
});

</script>