<template>
  <div class="p-8">
    <!-- Back button -->
    <button @click="router.back()" class="text-blue-600 hover:underline mb-4">
      ← Return to project list
    </button>

    <!-- Loading / error / empty states -->
    <div v-if="loading" class="text-lg text-gray-500">Loading project details...</div>
    <div v-else-if="error" class="text-red-600">Load error: {{ error }}</div>

    <!-- Main content -->
    <div v-else-if="project" class="bg-white rounded-xl shadow-xl p-6">
      <!-- Project header -->
      <h1 class="text-4xl font-bold text-blue-800 mb-2">{{ project.name }}</h1>
      <p class="text-gray-600 mb-6">
        Project ID: <span class="font-mono">{{ project.projectId }}</span> |
        Owner: <span class="font-semibold">{{ project.ownerUsername }}</span> |
        Visibility:
        <span :class="project.visibility === 'PUBLIC' ? 'text-green-600' : 'text-yellow-600'">
          {{ project.visibility }}
        </span>
      </p>

      <!-- Description -->
      <div class="border-t pt-4">
        <h2 class="text-2xl font-semibold mb-2">Project Description</h2>
        <p class="text-gray-800 leading-relaxed">{{ project.description }}</p>
      </div>

      <!-- Upload section -->
      <div class="mt-10 border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4">Upload Project Files</h2>

        <!-- Document upload -->
        <div class="mb-4">
          <p class="font-semibold mb-1">Upload document (.pdf / .docx)</p>
          <input type="file" ref="fileUploadInput" @change="handleFileSelect" />
          <button
            @click="uploadFile"
            class="ml-3 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Upload Document
          </button>
          <p v-if="fileMsg" class="mt-2 text-sm">{{ fileMsg }}</p>
        </div>

        <!-- Image upload -->
        <div class="mb-4">
          <p class="font-semibold mb-1">Upload image (.jpg / .png)</p>
          <input type="file" ref="imageUploadInput" @change="handleImageSelect" />
          <button
            @click="uploadImage"
            class="ml-3 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
          >
            Upload Image
          </button>
          <p v-if="imageMsg" class="mt-2 text-sm">{{ imageMsg }}</p>
        </div>
      </div>

      <!-- File list -->
      <div class="mt-10 border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4">Uploaded Files</h2>

        <div v-if="files.length === 0" class="text-gray-500">No files available.</div>

        <div class="grid grid-cols-2 gap-6">
          <div
            v-for="file in files"
            :key="file.fileId || file.imageId"
            class="p-4 bg-gray-50 rounded-lg shadow hover:shadow-md transition cursor-pointer relative"
            @click="openPreview(file)"
          >
            <p class="font-semibold text-blue-700">{{ file.fileName || file.imageName }}</p>
            <p class="text-sm text-gray-500 mt-1">Type: {{ file.fileType || 'image' }}</p>
            <p class="text-xs text-gray-400">Upload time: {{ file.uploadTimestamp }}</p>
            <p class="text-xs text-gray-400">Uploader: {{ file.uploaderUsername || 'Unknown' }}</p>
            
            <!-- Delete button -->
            <button
              v-if="canDelete(file)"
              @click.stop="deleteFile(file)"
              class="absolute top-2 right-2 text-red-500 hover:text-red-700 transition"
              title="Delete file"
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="bi bi-trash" viewBox="0 0 16 16">
                <path d="M5.5 5.5A.5.5 0 0 1 6 6v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm2.5 0a.5.5 0 0 1 .5.5v6a.5.5 0 0 1-1 0V6a.5.5 0 0 1 .5-.5zm3 .5a.5.5 0 0 0-1 0v6a.5.5 0 0 0 1 0V6z"/>
                <path fill-rule="evenodd" d="M14.5 3a1 1 0 0 1-1 1H13v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V4h-.5a1 1 0 0 1-1-1V2a1 1 0 0 1 1-1H6a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1h3.5a1 1 0 0 1 1 1v1zM4.118 4 4 4.059V13a1 1 0 0 0 1 1h6a1 1 0 0 0 1-1V4.059L11.882 4H4.118zM2.5 3V2h11v1h-11z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- Knowledge Graph -->
      <div class="mt-10 border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4 flex items-center gap-2">
          Knowledge Graph Visualization
        </h2>
        <p class="text-gray-600 mb-4 text-sm">
          This knowledge graph is generated from extracted entities and relations.
        </p>
        <div class="bg-gray-50 rounded-lg shadow-inner p-3">
          <iframe
            src="http://localhost:8000"
            class="w-full h-[600px] rounded-lg border"
            frameborder="0"
          ></iframe>
        </div>
      </div>

      <!-- Preview modal -->
      <div
        v-if="previewUrl"
        class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
      >
        <div class="bg-white p-4 rounded-xl w-3/4 h-3/4 relative shadow-xl">
          <button
            @click="previewUrl = ''"
            class="absolute top-3 right-3 px-3 py-1 bg-red-500 text-white rounded-lg"
          >
            ✕ Close
          </button>

          <!-- Bottom-right action buttons -->
          <div class="absolute bottom-4 right-4 flex gap-3 z-50">
            <!-- AI analysis -->
            <button
              v-if="currentPreviewFile && currentPreviewFile.fileId"
              @click="startAIAnalysis"
              :disabled="analyzing"
              class="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50"
            >
              {{ analyzing ? 'Analyzing...' : 'AI Smart Analysis' }}
            </button>

            <!-- View knowledge graph -->
            <button
              v-if="analysisStatus === 'COMPLETED'"
              @click="loadKnowledgeGraph"
              class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
            >
              View Knowledge Graph
            </button>

            <!-- Download report -->
            <button
              v-if="analysisStatus === 'COMPLETED'"
              @click="downloadAnalysisExcel"
              class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700"
            >
              Download Report
            </button>
          </div>

          <!-- Knowledge graph panel (inside modal) -->
          <div v-if="showGraph" class="mt-4 border-t pt-4">
            <h3 class="text-lg font-semibold mb-2">Knowledge Graph</h3>
            <div v-if="graphData" class="border rounded-lg p-4 max-h-[500px] overflow-auto">
              <h4 class="font-semibold mb-2">Nodes (Entities)</h4>
              <ul class="list-disc pl-5 text-sm text-gray-700 mb-4">
                <li v-for="node in graphData.nodes" :key="node.id">
                  {{ node.id }} <span class="text-gray-400">({{ node.type }})</span>
                </li>
              </ul>

              <h4 class="font-semibold mb-2">Relations</h4>
              <ul class="list-disc pl-5 text-sm text-gray-700">
                <li v-for="(edge, index) in graphData.edges" :key="index">
                  {{ edge.source }}
                  <span class="text-blue-600 font-semibold">
                    — {{ edge.relation }} →
                  </span>
                  {{ edge.target }}
                </li>
              </ul>
            </div>
          </div>

          <!-- AI status message -->
          <p
            v-if="analysisMsg"
            class="absolute bottom-4 left-4 text-sm font-bold text-purple-700 z-50 bg-white bg-opacity-80 px-2 py-1 rounded shadow-sm"
          >
            {{ analysisMsg }}
          </p>

          <!-- AI results -->
          <div v-if="analysisStatus === 'COMPLETED'" class="mt-4 p-4 border-t">
            <h3 class="text-lg font-semibold mb-2">AI Literature Summary</h3>
            <p class="text-gray-700 mb-4 leading-relaxed">
              {{ analysisSummary }}
            </p>

            <h3 class="text-lg font-semibold mb-2">Structured Feature Data</h3>
            <div class="overflow-auto max-h-64 border rounded-lg">
              <table class="min-w-full text-sm border-collapse">
                <thead class="bg-gray-100 sticky top-0">
                  <tr>
                    <th
                      v-for="(value, key) in analysisTableData[0]"
                      :key="key"
                      class="px-3 py-2 border text-left font-semibold"
                    >
                      {{ key }}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in analysisTableData" :key="index">
                    <td
                      v-for="(value, key) in row"
                      :key="key"
                      class="px-3 py-2 border"
                    >
                      {{ value }}
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- File preview -->
          <iframe
            v-if="previewType.includes('pdf') && analysisStatus !== 'COMPLETED'"
            :src="previewUrl"
            class="w-full h-full"
          ></iframe>

          <img
            v-else-if="previewType.includes('image')"
            :src="previewUrl"
            class="max-w-full max-h-full mx-auto"
          />
        </div>
      </div>
    </div>

    <!-- Empty project -->
    <div v-else class="text-lg text-gray-500">Project data is empty.</div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';

/* ---------- reactive refs ---------- */
const route = useRoute();
const router = useRouter();
const projectId = route.params.id;

const project = ref(null);
const files = ref([]); // merged files & images
const loading = ref(true);
const error = ref('');

/* preview */
const previewUrl = ref('');
const previewType = ref('');
const currentBlobUrl = ref('');

/* upload */
const selectedFile = ref(null);
const selectedImage = ref(null);
const fileMsg = ref('');
const imageMsg = ref('');
const fileUploadInput = ref(null);
const imageUploadInput = ref(null);

/* AI analysis */
const analyzing = ref(false);
const analysisMsg = ref('');
const currentPreviewFile = ref(null);

const analysisStatus = ref(''); // PROCESSING | COMPLETED | FAILED
const analysisSummary = ref('');
const analysisTableData = ref([]);
let analysisTimer = null;

/* knowledge graph */
const graphData = ref(null);
const showGraph = ref(false);

/* ---------- methods ---------- */
async function fetchProjectDetail() {
  try {
    const res = await request.get(`/projects/${projectId}`);
    if (res.code === 200) {
      project.value = res.data;
    } else {
      error.value = res.message;
    }
  } catch {
    error.value = 'Server error while fetching project details.';
  }
}

/* fetch files & images with isolated error handling */
async function fetchFiles() {
  files.value = [];

  /* files */
  try {
    const res = await request.get(`/projects/${projectId}/files`);
    if (res.code === 200 && res.data) files.value.push(...res.data);
    else console.warn('File list failed (non-200):', res.message);
  } catch (err) {
    console.error(`File list API error: ${err.message}`);
  }

  /* images */
  try {
    const img = await request.get(`/projects/${projectId}/images`);
    if (img.code === 200 && img.data) files.value.push(...img.data);
    else console.warn('Image list failed (non-200):', img.message);
  } catch (err) {
    console.error(`Image list API error: ${err.message}`);
  }
}

/* open preview with blob URL to keep auth header */
async function openPreview(file) {
  currentPreviewFile.value = file;

  /* revoke previous blob URL */
  if (currentBlobUrl.value) {
    URL.revokeObjectURL(currentBlobUrl.value);
    currentBlobUrl.value = '';
  }

  const relativeUrl = file.fileUrl || file.imageUrl;
  let type = file.fileType || 'image';
  let fullApiUrl = relativeUrl;

  /* build full URL */
  if (relativeUrl && !relativeUrl.startsWith('http') && !relativeUrl.startsWith('/api/v1')) {
    fullApiUrl = `${relativeUrl}`;
  }

  previewUrl.value = '';
  previewType.value = type;

  try {
    const response = await request.get(fullApiUrl, { responseType: 'blob' });
    const mimeType = response.type;

    if (mimeType.includes('image') || mimeType.includes('pdf')) {
      const blobUrl = URL.createObjectURL(response);
      currentBlobUrl.value = blobUrl;
      previewUrl.value = blobUrl;
      previewType.value = mimeType.includes('image') ? 'image' : 'pdf';
    } else {
      previewUrl.value = fullApiUrl;
      previewType.value = 'unsupported';
    }
  } catch (err) {
    console.error(`File load failed (403/auth): ${fullApiUrl}`, err);
    previewUrl.value = fullApiUrl;
    previewType.value = 'unsupported';
  }
}

/* release blob memory when modal closed */
watch(previewUrl, (newVal) => {
  if (!newVal && currentBlobUrl.value) {
    URL.revokeObjectURL(currentBlobUrl.value);
    currentBlobUrl.value = '';
    console.log('Blob memory released');
  }
  if (!newVal && analysisTimer) {
    clearInterval(analysisTimer);
    analysisTimer = null;
  }
});

/* file select handlers */
function handleFileSelect(e) {
  selectedFile.value = e.target.files[0];
}
function handleImageSelect(e) {
  selectedImage.value = e.target.files[0];
}

/* upload handlers */
async function uploadFile() {
  if (!selectedFile.value) {
    fileMsg.value = 'Please select a file first.';
    return;
  }
  fileMsg.value = 'Uploading...';
  const form = new FormData();
  form.append('file', selectedFile.value);
  try {
    const res = await request.post(`/projects/${projectId}/files`, form);
    if (res.code === 200) {
      fileMsg.value = 'Upload successful!';
      selectedFile.value = null;
      if (fileUploadInput.value) fileUploadInput.value.value = '';
      fetchFiles();
    } else {
      fileMsg.value = 'Upload failed. ' + res.message;
    }
  } catch (err) {
    fileMsg.value = 'Upload failed due to server error.';
    console.error('File upload error:', err);
  }
}

async function uploadImage() {
  if (!selectedImage.value) {
    imageMsg.value = 'Please select an image first.';
    return;
  }
  imageMsg.value = 'Uploading...';
  const form = new FormData();
  form.append('image', selectedImage.value);
  try {
    const res = await request.post(`/projects/${projectId}/images`, form);
    if (res.code === 200) {
      imageMsg.value = 'Upload successful!';
      selectedImage.value = null;
      if (imageUploadInput.value) imageUploadInput.value.value = '';
      fetchFiles();
    } else {
      imageMsg.value = 'Upload failed: ' + res.message;
    }
  } catch (err) {
    imageMsg.value = 'Upload failed due to server error.';
    console.error('Image upload error:', err);
  }
}

/* AI analysis */
async function startAIAnalysis() {
  if (!currentPreviewFile.value?.fileId) {
    analysisMsg.value = 'This file does not support AI analysis.';
    return;
  }
  analyzing.value = true;
  analysisMsg.value = 'Submitting AI analysis task...';
  analysisStatus.value = '';
  analysisSummary.value = '';
  analysisTableData.value = [];

  try {
    const res = await request.post(`/files/${currentPreviewFile.value.fileId}/analysis`);
    if (res.code === 200) {
      analysisMsg.value = res.message || 'Task submitted, preparing analysis...';
      analysisTimer = setInterval(() => pollAnalysisStatus(currentPreviewFile.value.fileId), 2000);
    } else {
      analysisMsg.value = res.message || 'Failed to start';
      analyzing.value = false;
    }
  } catch (err) {
    console.error('AI analysis error:', err);
    analysisMsg.value = 'Server error, unable to start analysis.';
    analyzing.value = false;
  }
}

async function pollAnalysisStatus(fileId) {
  try {
    const res = await request.get(`/files/${fileId}/analysis`);
    if (res.code !== 200 || !res.data) return;

    analysisStatus.value = res.data.status;

    if (res.data.status === 'PROCESSING') {
      analysisMsg.value = res.message || 'AI is analyzing the literature, please wait...';
    }
    if (res.data.status === 'COMPLETED') {
      analysisSummary.value = res.data.summary;
      analysisTableData.value = res.data.tableData || [];
      analysisMsg.value = '✅ AI analysis completed!';
      stopPolling();
    }
    if (res.data.status === 'FAILED') {
      analysisMsg.value = res.data.errorReason || '❌ AI analysis failed';
      stopPolling();
    }
  } catch (err) {
    console.error('Polling error:', err);
    analysisMsg.value = 'Polling error, please check network';
    stopPolling();
  }
}

function stopPolling() {
  if (analysisTimer) {
    clearInterval(analysisTimer);
    analysisTimer = null;
  }
  analyzing.value = false;
}

/* download Excel report */
async function downloadAnalysisExcel() {
  if (!currentPreviewFile.value?.fileId) {
    analysisMsg.value = 'Cannot download: file not found';
    return;
  }
  try {
    const response = await request.get(
      `/files/${currentPreviewFile.value.fileId}/analysis/download`,
      { responseType: 'blob' }
    );
    let filename = `Analysis_Report_${currentPreviewFile.value.fileId}.xlsx`;
    const disposition = response.headers?.['content-disposition'];
    if (disposition && disposition.includes('filename=')) {
      filename = decodeURIComponent(
        disposition.split('filename=')[1].replace(/"/g, '')
      );
    }
    const blob = new Blob([response], { type: response.type });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    console.error('Download Excel error:', err);
    analysisMsg.value =
      err?.response?.data?.message || 'Download failed, please ensure analysis is complete';
  }
}

/* load knowledge graph */
async function loadKnowledgeGraph() {
  if (!currentPreviewFile.value?.fileId) return;
  try {
    const res = await request.get(`/api/v1/graphs/${currentPreviewFile.value.fileId}/visualization`);
    if (res.code === 200 && res.data) {
      graphData.value = res.data;
      showGraph.value = true;
    } else {
      alert('Knowledge graph data is empty');
    }
  } catch (e) {
    console.error(e);
    alert('Failed to load knowledge graph');
  }
}

/* ---------- methods ---------- */
// 检查用户是否有权限删除文件
function canDelete(file) {
  // 获取当前用户信息
  const userStr = localStorage.getItem('user');
  if (!userStr) return false;
  
  const user = JSON.parse(userStr);
  const currentUsername = user.username;
  
  // 检查是否是文件上传者
  if (file.uploaderUsername === currentUsername) {
    return true;
  }
  
  // 检查是否是项目所有者
  if (project.value && project.value.ownerUsername === currentUsername) {
    return true;
  }
  
  return false;
}

// 删除文件
async function deleteFile(file) {
  if (!confirm('确定要删除这个文件吗？')) {
    return;
  }
  
  try {
    if (file.fileId) {
      // 删除文件
      const res = await request.post(`/files/${file.fileId}/delete`);
      if (res.code === 200) {
        alert('文件删除成功');
        await fetchFiles(); // 重新获取文件列表
      } else {
        alert('删除失败: ' + res.message);
      }
    } else if (file.imageId) {
      // 删除图片
      const res = await request.post(`/images/${file.imageId}/delete`);
      if (res.code === 200) {
        alert('图片删除成功');
        await fetchFiles(); // 重新获取文件列表
      } else {
        alert('删除失败: ' + res.message);
      }
    }
  } catch (err) {
    console.error('删除文件失败:', err);
    alert('删除失败，请检查网络连接');
  }
}

/* ---------- lifecycle ---------- */
onMounted(async () => {
  await fetchProjectDetail();
  await fetchFiles();
  loading.value = false;
});
</script>

<style scoped>
/* Add any scoped styles here */
</style>