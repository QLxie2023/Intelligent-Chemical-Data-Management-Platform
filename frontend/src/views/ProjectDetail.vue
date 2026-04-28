<template>
  <div class="p-8">
    <button @click="router.back()" class="text-blue-600 hover:underline mb-4">
      ← Return to project list
    </button>

    <div v-if="loading" class="text-lg text-gray-500">Loading project details...</div>
    <div v-else-if="error" class="text-red-600">Load error: {{ error }}</div>

    <div v-else-if="project" class="bg-white rounded-xl shadow-xl p-6">
      <h1 class="text-4xl font-bold text-blue-800 mb-2">{{ project.name }}</h1>
      <p class="text-gray-600 mb-6">
        Project ID: <span class="font-mono">{{ project.projectId }}</span> |
        Owner: <span class="font-semibold">{{ project.ownerUsername }}</span> |
        Visibility:
        <span :class="project.visibility === 'PUBLIC' ? 'text-green-600' : 'text-yellow-600'">
          {{ project.visibility }}
        </span>
      </p>

      <div class="border-t pt-4">
        <h2 class="text-2xl font-semibold mb-2">Project Description</h2>
        <p class="text-gray-800 leading-relaxed">{{ project.description }}</p>
      </div>

      <div class="mt-10 border-t pt-6">
        <h2 class="text-2xl font-semibold mb-4">Upload Project Files</h2>

        <div class="mb-4">
          <p class="font-semibold mb-1">Upload document (.pdf / .docx)</p>
          <input type="file" ref="fileUploadInput" @change="handleFileSelect" />
          <button
            @click="uploadFile"
            class="ml-3 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            Upload Document
          </button>
          <p v-if="fileMsg" class="mt-2 text-sm" :class="fileMsg.includes('successful') ? 'text-green-600' : 'text-gray-600'">{{ fileMsg }}</p>
        </div>

        <div class="mb-4">
          <p class="font-semibold mb-1">Upload image (.jpg / .png)</p>
          <input type="file" ref="imageUploadInput" @change="handleImageSelect" />
          <button
            @click="uploadImage"
            class="ml-3 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
          >
            Upload Image
          </button>
          <p v-if="imageMsg" class="mt-2 text-sm" :class="imageMsg.includes('successful') ? 'text-green-600' : 'text-gray-600'">{{ imageMsg }}</p>
        </div>
      </div>

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
            <span
              v-if="file._analysisStatus === 'PROCESSING' || file._analysisStatus === 'PENDING'"
              class="absolute top-2 right-2 px-2 py-0.5 bg-yellow-100 text-yellow-700 text-xs rounded-full"
            >Analyzing...</span>
            <span
              v-else-if="file._analysisStatus === 'COMPLETED'"
              class="absolute top-2 right-2 px-2 py-0.5 bg-green-100 text-green-700 text-xs rounded-full"
            >Analyzed</span>
            <span
              v-else-if="file._analysisStatus === 'FAILED'"
              class="absolute top-2 right-2 px-2 py-0.5 bg-red-100 text-red-700 text-xs rounded-full"
            >Failed</span>
          </div>
        </div>
      </div>

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
        <div class="bg-white p-4 rounded-xl w-3/4 h-3/4 relative shadow-xl flex flex-col">
          <button
            @click="closePreview"
            class="absolute top-3 right-3 px-3 py-1 bg-red-500 text-white rounded-lg z-50"
          >
            ✕ Close
          </button>

          <!-- Main content area: split into preview + analysis -->
          <div class="flex flex-1 min-h-0 mt-8">
            <!-- Left: file preview -->
            <div class="w-1/2 pr-2 overflow-auto">
              <iframe
                v-if="previewType.includes('pdf')"
                :src="previewUrl"
                class="w-full h-full"
              ></iframe>
              <img
                v-else-if="previewType.includes('image')"
                :src="previewUrl"
                class="max-w-full max-h-full mx-auto"
              />
            </div>

            <!-- Right: AI analysis results (editable) -->
            <div class="w-1/2 pl-2 border-l overflow-auto">
              <!-- Processing state -->
              <div v-if="analysisStatus === 'PROCESSING'" class="flex flex-col items-center justify-center h-full">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mb-4"></div>
                <p class="text-purple-700 font-semibold">AI is analyzing the file, please wait...</p>
                <p class="text-sm text-gray-500 mt-2">{{ analysisMsg }}</p>
              </div>

              <!-- Failed state -->
              <div v-else-if="analysisStatus === 'FAILED'" class="flex flex-col items-center justify-center h-full">
                <p class="text-red-600 font-semibold text-lg mb-2">Analysis Failed</p>
                <p class="text-sm text-gray-500">{{ analysisMsg }}</p>
              </div>

              <!-- PENDING state -->
              <div v-else-if="analysisStatus === 'PENDING'" class="flex flex-col items-center justify-center h-full">
                <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-600 mb-4"></div>
                <p class="text-purple-700 font-semibold">AI is analyzing, please wait...</p>
                <p class="text-sm text-gray-500 mt-2">{{ analysisMsg }}</p>
              </div>

              <!-- COMPLETED state: editable form -->
              <div v-else-if="analysisStatus === 'COMPLETED'" class="p-4">
                <div class="flex items-center justify-between mb-4">
                  <h3 class="text-lg font-semibold text-purple-800">AI Analysis Results</h3>
                  <span class="text-xs text-gray-400">You can edit the results below</span>
                </div>

                <!-- Standardized Name -->
                <div class="mb-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-1">Standardized Name</label>
                  <input
                    v-model="editForm.standardizedName"
                    type="text"
                    class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-400 focus:outline-none"
                  />
                </div>

                <!-- Summary -->
                <div class="mb-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-1">Summary</label>
                  <textarea
                    v-model="editForm.summary"
                    rows="4"
                    class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-400 focus:outline-none"
                  ></textarea>
                </div>

                <!-- Data Description -->
                <div class="mb-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-1">Data Description</label>
                  <textarea
                    v-model="editForm.dataDescription"
                    rows="3"
                    class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-purple-400 focus:outline-none"
                  ></textarea>
                </div>

                <!-- Keywords -->
                <div class="mb-4">
                  <label class="block text-sm font-semibold text-gray-700 mb-1">Keywords</label>
                  <div class="flex flex-wrap gap-2 mb-2">
                    <span
                      v-for="(kw, idx) in editForm.keywords"
                      :key="idx"
                      class="inline-flex items-center px-3 py-1 bg-purple-100 text-purple-800 rounded-full text-sm"
                    >
                      {{ kw }}
                      <button @click="removeKeyword(idx)" class="ml-1 text-purple-500 hover:text-red-500">&times;</button>
                    </span>
                  </div>
                  <div class="flex gap-2">
                    <input
                      v-model="newKeyword"
                      @keyup.enter="addKeyword"
                      type="text"
                      placeholder="Add keyword"
                      class="flex-1 px-3 py-1 border rounded-lg text-sm focus:ring-2 focus:ring-purple-400 focus:outline-none"
                    />
                    <button @click="addKeyword" class="px-3 py-1 bg-purple-100 text-purple-700 rounded-lg text-sm hover:bg-purple-200">Add</button>
                  </div>
                </div>

                <!-- Table Data (structured feature data) -->
                <div class="mb-4" v-if="editForm.tableData.length > 0">
                  <label class="block text-sm font-semibold text-gray-700 mb-1">Structured Feature Data</label>
                  <div class="overflow-auto max-h-64 border rounded-lg">
                    <table class="min-w-full text-sm border-collapse">
                      <thead class="bg-gray-100 sticky top-0">
                        <tr>
                          <th
                            v-for="key in tableHeaders"
                            :key="key"
                            class="px-3 py-2 border text-left font-semibold"
                          >
                            {{ key }}
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(row, index) in editForm.tableData" :key="index">
                          <td
                            v-for="key in tableHeaders"
                            :key="key"
                            class="px-2 py-1 border"
                          >
                            <input
                              v-model="editForm.tableData[index][key]"
                              class="w-full px-1 py-0.5 border-0 bg-transparent focus:bg-white focus:ring-1 focus:ring-purple-400 focus:outline-none text-sm"
                            />
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <!-- Save button -->
                <div class="flex gap-3 mt-4">
                  <button
                    @click="saveAnalysisResult"
                    :disabled="saving"
                    :class="editForm.isConfirmed
                      ? 'px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition'
                      : 'px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 disabled:opacity-50 transition'"
                  >
                    {{ saving ? 'Saving...' : (editForm.isConfirmed ? 'Update Confirmed Results' : 'Confirm & Save Results') }}
                  </button>
                  <button
                    @click="reAnalyze(currentFileId)"
                    :disabled="analysisStatus === 'PROCESSING'"
                    class="px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 disabled:opacity-50 transition"
                  >
                    Re-analyze
                  </button>
                  <button
                    @click="downloadAnalysisExcel"
                    class="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition"
                  >
                    Download Report
                  </button>
                  <button
                    @click="loadKnowledgeGraph"
                    class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                  >
                    View Knowledge Graph
                  </button>
                </div>

                <!-- Save status message -->
                <p v-if="saveMsg" class="mt-2 text-sm" :class="saveMsg.includes('success') ? 'text-green-600' : 'text-red-600'">
                  {{ saveMsg }}
                </p>

                <!-- Knowledge graph panel -->
                <div v-if="showGraph" class="mt-4 border-t pt-4">
                  <h4 class="font-semibold mb-2">Knowledge Graph</h4>
                  <div v-if="graphData" class="border rounded-lg p-4 max-h-[300px] overflow-auto">
                    <p class="font-semibold mb-1 text-sm">Nodes (Entities)</p>
                    <ul class="list-disc pl-5 text-sm text-gray-700 mb-3">
                      <li v-for="node in graphData.nodes" :key="node.id">
                        {{ node.id }} <span class="text-gray-400">({{ node.type }})</span>
                      </li>
                    </ul>
                    <p class="font-semibold mb-1 text-sm">Relations</p>
                    <ul class="list-disc pl-5 text-sm text-gray-700">
                      <li v-for="(edge, index) in graphData.edges" :key="index">
                        {{ edge.source }}
                        <span class="text-blue-600 font-semibold">— {{ edge.relation }} →</span>
                        {{ edge.target }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- No status yet -->
              <div v-else class="flex flex-col items-center justify-center h-full">
                <p class="text-gray-400">Click a file to view analysis results</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-lg text-gray-500">Project data is empty.</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';

const route = useRoute();
const router = useRouter();
const projectId = route.params.id;

const project = ref(null);
const files = ref([]);
const loading = ref(true);
const error = ref('');

const previewUrl = ref('');
const previewType = ref('');
const currentBlobUrl = ref('');

const selectedFile = ref(null);
const selectedImage = ref(null);
const fileMsg = ref('');
const imageMsg = ref('');
const fileUploadInput = ref(null);
const imageUploadInput = ref(null);

const analysisMsg = ref('');
const currentPreviewFile = ref(null);
const currentFileId = ref(null);
const currentFileType = ref('file');

const analysisStatus = ref('');
const saving = ref(false);
const saveMsg = ref('');
let pollingFileId = null;
let reanalyzingFileIds = new Set();

const editForm = ref({
  summary: '',
  standardizedName: '',
  dataDescription: '',
  keywords: [],
  tableData: [],
  rawAnalysisData: ''
});

const newKeyword = ref('');

const graphData = ref(null);
const showGraph = ref(false);

let analysisTimer = null;
let globalStatusTimer = null;

const tableHeaders = computed(() => {
  if (editForm.value.tableData.length > 0) {
    return Object.keys(editForm.value.tableData[0]);
  }
  return [];
});

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

async function fetchFiles() {
  files.value = [];

  const allItems = [];

  try {
    const res = await request.get(`/projects/${projectId}/files`);
    if (res.code === 200 && res.data) {
      allItems.push(...res.data.map(f => ({ ...f, _analysisStatus: null, _itemType: 'file' })));
    }
  } catch (err) {
    console.error(`File list API error: ${err.message}`);
  }

  try {
    const img = await request.get(`/projects/${projectId}/images`);
    if (img.code === 200 && img.data) {
      allItems.push(...img.data.map(i => ({ ...i, _analysisStatus: null, _itemType: 'image' })));
    }
  } catch (err) {
    console.error(`Image list API error: ${err.message}`);
  }

  const statusPromises = allItems.map(async (item) => {
    const fileId = item.fileId || item.imageId;
    const ft = item.fileType ? 'file' : 'image';
    try {
      const res = await request.get(`/files/${fileId}/analysis?fileType=${ft}`);
      if (res.code === 200 && res.data) {
        item._analysisStatus = res.data.status || 'PENDING';
      } else {
        item._analysisStatus = 'PENDING';
      }
    } catch {
      item._analysisStatus = 'PENDING';
    }
  });

  await Promise.all(statusPromises);
  files.value = allItems;

  startGlobalStatusPolling();
}

async function openPreview(file) {
  currentPreviewFile.value = file;
  currentFileId.value = file.fileId || file.imageId;
  currentFileType.value = file.fileType ? 'file' : 'image';
  analysisStatus.value = 'PROCESSING';
  analysisMsg.value = 'Loading analysis status...';
  saveMsg.value = '';
  showGraph.value = false;
  resetEditForm();

  if (currentBlobUrl.value) {
    URL.revokeObjectURL(currentBlobUrl.value);
    currentBlobUrl.value = '';
  }

  const relativeUrl = file.fileUrl || file.imageUrl;
  let type = file.fileType || 'image';
  let fullApiUrl = relativeUrl;

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
    console.error(`File load failed: ${fullApiUrl}`, err);
    previewUrl.value = fullApiUrl;
    previewType.value = 'unsupported';
  }

  await loadAnalysisResults(currentFileId.value);
}

function closePreview() {
  previewUrl.value = '';
  stopPolling();
  if (currentFileId.value && reanalyzingFileIds.has(currentFileId.value) && analysisStatus.value === 'COMPLETED') {
    reanalyzingFileIds.delete(currentFileId.value);
  }
}

function resetEditForm() {
  editForm.value = {
    summary: '',
    standardizedName: '',
    dataDescription: '',
    keywords: [],
    tableData: [],
    rawAnalysisData: '',
    isConfirmed: false
  };
}

async function loadAnalysisResults(fileId) {
  try {
    const ft = currentFileType.value || 'file';
    const res = await request.get(`/files/${fileId}/analysis?fileType=${ft}`);
    if (res.code !== 200 || !res.data) {
      analysisStatus.value = '';
      return;
    }

    const data = res.data;
    analysisStatus.value = data.status;

    if (data.fileType) {
      currentFileType.value = data.fileType;
    }

    if (data.status === 'COMPLETED') {
      let sourceData = data;
      const isReanalyzing = reanalyzingFileIds.has(fileId);
      if (!isReanalyzing && data.confirmedData) {
        try {
          sourceData = JSON.parse(data.confirmedData);
        } catch (e) {
          console.error('Parse confirmedData failed:', e);
          sourceData = data;
        }
      }
      editForm.value.summary = sourceData.summary || data.summary || '';
      editForm.value.standardizedName = sourceData.standardized_name || data.standardized_name || '';
      editForm.value.dataDescription = sourceData.data_description || data.data_description || '';
      editForm.value.keywords = Array.isArray(sourceData.keywords) ? [...sourceData.keywords] : (Array.isArray(data.keywords) ? [...data.keywords] : []);
      editForm.value.tableData = Array.isArray(sourceData.tableData) ? JSON.parse(JSON.stringify(sourceData.tableData)) : (Array.isArray(data.tableData) ? JSON.parse(JSON.stringify(data.tableData)) : []);
      editForm.value.rawAnalysisData = data.analysisData || '';
      editForm.value.isConfirmed = isReanalyzing ? false : !!data.confirmedData;
      analysisMsg.value = isReanalyzing
        ? 'AI re-analysis completed. You can review and edit the new results.'
        : (data.confirmedData
          ? 'Results confirmed and saved. You can still edit and re-save.'
          : 'AI analysis completed. You can review and edit the results.');
    } else if (data.status === 'PROCESSING') {
      analysisMsg.value = 'AI is analyzing the file, please wait...';
      startPolling(fileId);
    } else if (data.status === 'FAILED') {
      analysisMsg.value = data.errorReason || 'AI analysis failed';
    } else if (data.status === 'PENDING') {
      analysisStatus.value = 'PROCESSING';
      analysisMsg.value = 'AI is analyzing, please wait...';
      await triggerAnalysis(fileId);
    }
  } catch (err) {
    console.error('Load analysis error:', err);
    analysisStatus.value = '';
  }
}

function startPolling(fileId) {
  stopPolling();
  pollingFileId = fileId;
  analysisTimer = setInterval(() => pollAnalysisStatus(fileId), 2000);
}

async function triggerAnalysis(fileId) {
  try {
    const res = await request.post(`/files/${fileId}/analysis?fileType=${currentFileType.value || 'file'}`);
    if (res.code === 200) {
      analysisStatus.value = 'PROCESSING';
      analysisMsg.value = 'AI is analyzing the file, please wait...';
      startPolling(fileId);
    } else {
      analysisMsg.value = res.message || 'Failed to start analysis';
    }
  } catch (err) {
    console.error('Trigger analysis error:', err);
    analysisMsg.value = 'Failed to trigger analysis';
  }
}

async function reAnalyze(fileId) {
  if (!fileId) return;
  try {
    editForm.value.summary = '';
    editForm.value.standardizedName = '';
    editForm.value.dataDescription = '';
    editForm.value.keywords = [];
    editForm.value.tableData = [];
    editForm.value.isConfirmed = false;
    analysisStatus.value = 'PROCESSING';
    analysisMsg.value = 'Re-analyzing with AI, please wait...';

    reanalyzingFileIds.add(fileId);

    const fileItem = files.value.find(f => (f.fileId || f.imageId) === fileId);
    if (fileItem) fileItem._analysisStatus = 'PROCESSING';
    startGlobalStatusPolling();

    const res = await request.post(`/files/${fileId}/analysis?fileType=${currentFileType.value || 'file'}`);
    if (res.code === 200) {
      startPolling(fileId);
    } else {
      analysisMsg.value = res.message || 'Failed to re-analyze';
    }
  } catch (err) {
    console.error('Re-analyze error:', err);
    analysisMsg.value = 'Failed to re-analyze';
  }
}

async function pollAnalysisStatus(fileId) {
  try {
    const ft = currentFileType.value || 'file';
    const res = await request.get(`/files/${fileId}/analysis?fileType=${ft}`);
    if (res.code !== 200 || !res.data) return;

    const data = res.data;
    analysisStatus.value = data.status;

    if (data.fileType) {
      currentFileType.value = data.fileType;
    }

    if (data.status === 'PROCESSING') {
      analysisMsg.value = 'AI is analyzing the file, please wait...';
      const fileItem = files.value.find(f => (f.fileId || f.imageId) === fileId);
      if (fileItem) fileItem._analysisStatus = 'PROCESSING';
    }
    if (data.status === 'COMPLETED') {
      let sourceData = data;
      const isReanalyzing = reanalyzingFileIds.has(fileId);
      if (!isReanalyzing && data.confirmedData) {
        try {
          sourceData = JSON.parse(data.confirmedData);
        } catch (e) {
          console.error('Parse confirmedData failed:', e);
          sourceData = data;
        }
      }
      editForm.value.summary = sourceData.summary || data.summary || '';
      editForm.value.standardizedName = sourceData.standardized_name || data.standardized_name || '';
      editForm.value.dataDescription = sourceData.data_description || data.data_description || '';
      editForm.value.keywords = Array.isArray(sourceData.keywords) ? [...sourceData.keywords] : (Array.isArray(data.keywords) ? [...data.keywords] : []);
      editForm.value.tableData = Array.isArray(sourceData.tableData) ? JSON.parse(JSON.stringify(sourceData.tableData)) : (Array.isArray(data.tableData) ? JSON.parse(JSON.stringify(data.tableData)) : []);
      editForm.value.rawAnalysisData = data.analysisData || '';
      editForm.value.isConfirmed = isReanalyzing ? false : !!data.confirmedData;
      analysisMsg.value = isReanalyzing
        ? 'AI re-analysis completed. You can review and edit the new results.'
        : (data.confirmedData
          ? 'Results confirmed and saved. You can still edit and re-save.'
          : 'AI analysis completed. You can review and edit the results.');
      stopPolling();

      const fileItem = files.value.find(f => (f.fileId || f.imageId) === fileId);
      if (fileItem) fileItem._analysisStatus = 'COMPLETED';
    }
    if (data.status === 'FAILED') {
      analysisMsg.value = data.errorReason || 'AI analysis failed';
      stopPolling();

      const fileItem = files.value.find(f => (f.fileId || f.imageId) === fileId);
      if (fileItem) fileItem._analysisStatus = 'FAILED';
    }
  } catch (err) {
    console.error('Polling error:', err);
    stopPolling();
  }
}

function stopPolling() {
  if (analysisTimer) {
    clearInterval(analysisTimer);
    analysisTimer = null;
  }
  pollingFileId = null;
}

function startGlobalStatusPolling() {
  stopGlobalStatusPolling();
  globalStatusTimer = setInterval(pollAllFileStatuses, 3000);
}

function stopGlobalStatusPolling() {
  if (globalStatusTimer) {
    clearInterval(globalStatusTimer);
    globalStatusTimer = null;
  }
}

async function pollAllFileStatuses() {
  const processingItems = files.value.filter(
    f => f._analysisStatus === 'PROCESSING' || f._analysisStatus === 'PENDING'
  );
  if (processingItems.length === 0) {
    stopGlobalStatusPolling();
    return;
  }

  const promises = processingItems.map(async (item) => {
    const fileId = item.fileId || item.imageId;
    const ft = item.fileType ? 'file' : 'image';
    try {
      const res = await request.get(`/files/${fileId}/analysis?fileType=${ft}`);
      if (res.code === 200 && res.data) {
        const newStatus = res.data.status;
        if (item._analysisStatus !== newStatus) {
          item._analysisStatus = newStatus;
        }
      }
    } catch {}
  });

  await Promise.all(promises);

  const stillProcessing = files.value.some(
    f => f._analysisStatus === 'PROCESSING' || f._analysisStatus === 'PENDING'
  );
  if (!stillProcessing) {
    stopGlobalStatusPolling();
  }
}

function addKeyword() {
  const kw = newKeyword.value.trim();
  if (kw && !editForm.value.keywords.includes(kw)) {
    editForm.value.keywords.push(kw);
  }
  newKeyword.value = '';
}

function removeKeyword(idx) {
  editForm.value.keywords.splice(idx, 1);
}

async function saveAnalysisResult() {
  if (!currentFileId.value) return;

  saving.value = true;
  saveMsg.value = '';

  try {
    const confirmedObj = {
      summary: editForm.value.summary,
      standardized_name: editForm.value.standardizedName,
      data_description: editForm.value.dataDescription,
      keywords: editForm.value.keywords,
      tableData: editForm.value.tableData
    };

    const confirmedDataStr = JSON.stringify(confirmedObj);

    const res = await request.put(`/files/${currentFileId.value}/analysis`, {
      confirmedData: confirmedDataStr,
      fileType: currentFileType.value
    });

    if (res.code === 200) {
      saveMsg.value = 'Results confirmed and saved successfully!';
      editForm.value.isConfirmed = true;
      reanalyzingFileIds.delete(currentFileId.value);
    } else {
      saveMsg.value = 'Save failed: ' + (res.message || 'Unknown error');
    }
  } catch (err) {
    console.error('Save error:', err);
    saveMsg.value = 'Save failed: server error';
  } finally {
    saving.value = false;
  }
}

function handleFileSelect(e) {
  selectedFile.value = e.target.files[0];
}
function handleImageSelect(e) {
  selectedImage.value = e.target.files[0];
}

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
      fileMsg.value = 'Upload successful! AI is analyzing automatically...';
      selectedFile.value = null;
      if (fileUploadInput.value) fileUploadInput.value.value = '';

      if (res.data && res.data.fileId) {
        currentPreviewFile.value = {
          fileId: res.data.fileId,
          fileName: res.data.fileName || 'Uploaded file',
          fileType: res.data.fileType || 'file'
        };
        currentFileId.value = res.data.fileId;
        currentFileType.value = 'file';
        analysisStatus.value = 'PROCESSING';
        analysisMsg.value = 'AI is analyzing the file, please wait...';
        startPolling(res.data.fileId);

        files.value.push({
          fileId: res.data.fileId,
          fileName: res.data.fileName || 'Uploaded file',
          fileType: res.data.fileType || 'file',
          fileUrl: res.data.fileUrl,
          uploadTimestamp: res.data.uploadTimestamp || new Date().toISOString(),
          _analysisStatus: 'PROCESSING',
          _itemType: 'file'
        });
        startGlobalStatusPolling();
      }
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
      imageMsg.value = 'Upload successful! AI is analyzing automatically...';
      selectedImage.value = null;
      if (imageUploadInput.value) imageUploadInput.value.value = '';

      if (res.data && res.data.imageId) {
        currentPreviewFile.value = {
          fileId: res.data.imageId,
          fileName: res.data.imageName || 'Uploaded image',
          fileType: 'image'
        };
        currentFileId.value = res.data.imageId;
        currentFileType.value = 'image';
        analysisStatus.value = 'PROCESSING';
        analysisMsg.value = 'AI is analyzing the image, please wait...';

        files.value.push({
          imageId: res.data.imageId,
          imageName: res.data.imageName || 'Uploaded image',
          imageUrl: res.data.imageUrl,
          uploadTimestamp: res.data.uploadTimestamp || new Date().toISOString(),
          _analysisStatus: 'PROCESSING',
          _itemType: 'image'
        });
        startGlobalStatusPolling();

        triggerAnalysis(res.data.imageId);
      }
    } else {
      imageMsg.value = 'Upload failed: ' + res.message;
    }
  } catch (err) {
    imageMsg.value = 'Upload failed due to server error.';
    console.error('Image upload error:', err);
  }
}

async function downloadAnalysisExcel() {
  if (!currentFileId.value) return;
  try {
    const ft = currentFileType.value || 'file';
    const response = await request.get(
      `/files/${currentFileId.value}/analysis/download?fileType=${ft}`,
      { responseType: 'blob' }
    );
    let filename = `Analysis_Report_${currentFileId.value}.xlsx`;
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
    saveMsg.value = 'Download failed, please ensure analysis is complete';
  }
}

async function loadKnowledgeGraph() {
  if (!currentFileId.value) return;
  try {
    const res = await request.get(`/api/v1/graphs/${currentFileId.value}/visualization`);
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

onMounted(async () => {
  await fetchProjectDetail();
  await fetchFiles();
  loading.value = false;
});

onUnmounted(() => {
  stopPolling();
  stopGlobalStatusPolling();
  if (currentBlobUrl.value) {
    URL.revokeObjectURL(currentBlobUrl.value);
  }
});
</script>

<style scoped>
</style>
