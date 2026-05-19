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
        <router-link class="nav-item" to="/dashboard">dashboard</router-link>
        <router-link class="nav-item" to="/project-management" exact>project management</router-link>
        <router-link class="nav-item" to="/user">user management</router-link>
        <!-- <router-link class="nav-item" to="/search">intelligent retrieval</router-link> -->
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
        <h1 class="text-3xl font-bold">Project Management</h1>

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
        <h2 class="text-xl font-semibold mb-4">Project List (total {{ projects.length }})</h2>

        <!-- Search bar -->
        <div class="mb-6 flex gap-3">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="Search by keyword (e.g., experiment topic, chemical name...)"
            class="flex-1 px-4 py-2 border rounded-lg"
            @keyup.enter="performSearch"
          />
          <button
            @click="performSearch"
            class="px-5 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Search
          </button>
          <button
            v-if="searchResults.length > 0"
            @click="clearSearch"
            class="px-4 py-2 bg-gray-300 rounded-lg hover:bg-gray-400 transition"
          >
            Clear
          </button>
        </div>

        <!-- Search results -->
        <div v-if="searchResults.length > 0" class="mb-8">
          <p class="text-sm text-gray-500 mb-3">
            Found {{ searchResults.length }} result(s) for "{{ searchKeyword }}"
          </p>
          <div class="grid gap-4">
            <div
              v-for="item in searchResults"
              :key="(item.fileId || item.imageId)"
              class="bg-white rounded-xl shadow p-5 hover:shadow-md transition cursor-pointer"
              @click="goToSearchResult(item)"
            >
              <div class="flex justify-between items-start mb-2">
                <h3 class="text-lg font-semibold text-blue-700">{{ item.fileName }}</h3>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="item.fileType === 'image' ? 'bg-green-100 text-green-700' : 'bg-blue-100 text-blue-700'"
                >
                  {{ item.fileType }}
                </span>
              </div>
              <p class="text-sm text-gray-600 mb-1">
                Project: <span class="font-medium">{{ item.projectName }}</span>
                &nbsp;|&nbsp; Owner: <span class="font-medium">{{ item.ownerUsername }}</span>
              </p>
              <p v-if="item.standardizedName" class="text-sm text-gray-500 mb-1">
                {{ item.standardizedName }}
              </p>
              <p v-if="item.summary" class="text-sm text-gray-600 mb-2 line-clamp-2">
                {{ item.summary.length > 200 ? item.summary.substring(0, 200) + '...' : item.summary }}
              </p>
              <div v-if="item.keywords && item.keywords.length > 0" class="flex flex-wrap gap-1">
                <span
                  v-for="kw in item.keywords"
                  :key="kw"
                  class="text-xs px-2 py-0.5 bg-purple-100 text-purple-700 rounded-full"
                >
                  {{ kw }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div v-else-if="searchPerformed && searchResults.length === 0" class="mb-8 text-gray-500">
          No results found for "{{ searchKeyword }}".
        </div>
        
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
                    <td class="px-6 py-4 text-sm text-gray-700">
                        <button 
                            v-if="canDeleteProject(project)" 
                            @click.stop="openDeleteConfirm(project)" 
                            class="px-3 py-1 rounded text-sm text-white bg-red-500 hover:bg-red-600 transition"
                            title="Delete project"
                        >
                            Delete
                        </button>
                    </td>
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
            <p class="font-semibold mb-2">Option: Upload experimental files (.pdf / .docx / .xlsx / .csv)</p>
            <input type="file" ref="fileInput" @change="selectFile" class="hidden" />
            <button type="button" @click="fileInput.click()" class="px-4 py-2 border rounded-lg hover:bg-gray-100 transition mb-3">Choose File</button>
            <p v-if="fileUploadMsg" :class="fileUploadMsg.includes('fail') ? 'text-red-600' : 'text-green-600'" class="text-sm mt-1">{{ fileUploadMsg }}</p>
        </div>

        <div class="mb-4 p-3 border rounded-lg">
            <p class="font-semibold mb-2">Option: Upload experimental images (.jpg / .png)</p>
            <input type="file" ref="imageInput" @change="selectImage" class="hidden" />
            <button type="button" @click="imageInput.click()" class="px-4 py-2 border rounded-lg hover:bg-gray-100 transition mb-3">Choose Image</button>
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

    <div
      v-if="showDeleteConfirm"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4"
      @click.self="cancelDelete"
    >
      <div class="w-full max-w-2xl min-h-[280px] rounded-2xl bg-white p-10 shadow-2xl">
        <div class="flex min-h-[120px] items-center justify-center rounded-xl border border-red-100 bg-red-50 px-8 py-8">
          <h2 class="text-center text-3xl font-bold text-gray-900">Delete this project?</h2>
        </div>

        <div class="mt-10 flex justify-center gap-5 border-t border-gray-100 pt-8">
          <button
            type="button"
            @click="cancelDelete"
            class="min-w-32 rounded-lg border border-gray-300 px-7 py-3 text-gray-700 hover:bg-gray-100 transition disabled:opacity-60"
            :disabled="isDeletingProject"
          >
            Cancel
          </button>
          <button
            type="button"
            @click="confirmDeleteProject"
            class="min-w-40 rounded-lg bg-red-600 px-7 py-3 text-white hover:bg-red-700 transition disabled:opacity-60"
            :disabled="isDeletingProject"
          >
            Confirm Delete
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
const showDeleteConfirm = ref(false);
const pendingDeleteProject = ref(null);
const isDeletingProject = ref(false);

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

// --- search ---
const searchKeyword = ref('');
const searchResults = ref([]);
const searchPerformed = ref(false);

const performSearch = async () => {
  const keyword = searchKeyword.value.trim();
  if (!keyword) return;
  searchPerformed.value = false;
  searchResults.value = [];
  try {
    const res = await request.get('/projects/search', { params: { keyword } });
    if (res.code === 200 && Array.isArray(res.data)) {
      searchResults.value = res.data;
    }
  } catch (err) {
    console.error('Search failed:', err);
  }
  searchPerformed.value = true;
};

const clearSearch = () => {
  searchKeyword.value = '';
  searchResults.value = [];
  searchPerformed.value = false;
};

const goToSearchResult = (item) => {
  const query = {};
  if (item.fileType === 'image') {
    query.imageId = item.imageId;
  } else {
    query.fileId = item.fileId;
  }
  router.push({ name: 'ProjectDetail', params: { id: item.projectId }, query });
};

// --- choose files/images ---
const selectedFile = ref(null);
const selectedImage = ref(null);

// Used for resetting the file input box
const fileInput = ref(null);
const imageInput = ref(null);


const selectFile = (e) => {
  selectedFile.value = e.target.files[0];
  fileUploadMsg.value = selectedFile.value ? `Selected file: ${selectedFile.value.name}` : '';
};

const selectImage = (e) => {
  selectedImage.value = e.target.files[0];
  imageUploadMsg.value = selectedImage.value ? `Selected image: ${selectedImage.value.name}` : '';
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

// 检查用户是否有权限删除项目
const canDeleteProject = (project) => {
  // 获取当前用户信息
  const userStr = localStorage.getItem('user');
  if (!userStr) return false;
  
  const user = JSON.parse(userStr);
  const currentUsername = user.username;
  
  // 检查是否是项目所有者
  if (project.ownerUsername === currentUsername) {
    return true;
  }
  
  return false;
};

// 删除项目
const openDeleteConfirm = (project) => {
  pendingDeleteProject.value = project;
  showDeleteConfirm.value = true;
};

const cancelDelete = () => {
  if (isDeletingProject.value) return;
  showDeleteConfirm.value = false;
  pendingDeleteProject.value = null;
};

const confirmDeleteProject = async () => {
  const project = pendingDeleteProject.value;
  if (!project) return;

  isDeletingProject.value = true;
  try {
    const res = await request.post(`/projects/${project.projectId}/delete`);
    if (res.code === 200) {
      alert('Project deleted successfully');
      showDeleteConfirm.value = false;
      pendingDeleteProject.value = null;
      fetchProjects();
    } else {
      alert('Delete failed: ' + res.message);
    }
  } catch (err) {
    console.error('Delete project failed:', err);
    alert('Delete failed, please check the network connection.');
  } finally {
    isDeletingProject.value = false;
  }
};

// Get project list
onMounted(() => {
  fetchProjects();
});

</script>
