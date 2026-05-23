<template>
  <div class="min-h-screen flex bg-gray-100">
    <aside class="z-10 w-64 bg-white shadow-lg flex flex-col">
      <div class="flex items-center gap-3 p-6">
        <img
          src="../assets/logo.png"
          alt="Chem+ Logo"
          class="w-10 h-10 rounded-full object-cover shadow"
        />
        <span class="text-2xl font-bold text-blue-700">Chem+</span>
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

    <main class="flex-1 p-8 overflow-auto">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h1 class="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p class="text-gray-500 mt-1">
            {{ usingMockData ? 'Mock data preview. Backend data will replace it when available.' : 'Live data from project file APIs.' }}
          </p>
        </div>
        <button
          @click="loadDashboardData"
          class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-60"
          :disabled="loading"
        >
          {{ loading ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>

      <div v-if="errorMsg" class="mb-4 px-4 py-3 bg-yellow-50 text-yellow-700 rounded-lg border border-yellow-200">
        {{ errorMsg }}
      </div>

      <section class="grid grid-cols-4 gap-5 mb-6">
        <div class="metric-card">
          <p class="metric-label">Projects</p>
          <p class="metric-value">{{ summary.totalProjects }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">Documents</p>
          <p class="metric-value">{{ summary.totalFiles }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">Images</p>
          <p class="metric-value">{{ summary.totalImages }}</p>
        </div>
        <div class="metric-card">
          <p class="metric-label">Analyzed</p>
          <p class="metric-value">{{ summary.analyzed }}</p>
        </div>
      </section>

      <section class="grid grid-cols-3 gap-6 mb-6">
        <div class="chart-panel col-span-2">
          <div class="panel-header">
            <h2>Files by Project</h2>
            <span>Total {{ summary.totalItems }}</span>
          </div>
          <div class="bar-chart">
            <div
              v-for="item in projectBars"
              :key="item.name"
              class="bar-row"
            >
              <span class="bar-name" :title="item.name">{{ item.name }}</span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: `${item.percent}%` }"
                ></div>
              </div>
              <span class="bar-count">{{ item.count }}</span>
            </div>
          </div>
        </div>

        <div class="chart-panel">
          <div class="panel-header">
            <h2>File Mix</h2>
            <span>Docs / Images</span>
          </div>
          <div class="pie-wrap">
            <div
              class="pie-chart"
              :style="{ background: pieBackground }"
            ></div>
            <div class="legend">
              <div><span class="legend-dot bg-blue-500"></span>Documents {{ summary.totalFiles }}</div>
              <div><span class="legend-dot bg-emerald-500"></span>Images {{ summary.totalImages }}</div>
            </div>
          </div>
        </div>
      </section>

      <section class="grid grid-cols-3 gap-6">
        <div class="chart-panel col-span-2">
          <div class="panel-header">
            <h2>Upload Trend</h2>
            <span>Last 7 days</span>
          </div>
          <svg viewBox="0 0 720 260" class="line-chart" role="img" aria-label="Upload trend line chart">
            <line
              v-for="tick in 5"
              :key="tick"
              x1="40"
              x2="700"
              :y1="30 + (tick - 1) * 45"
              :y2="30 + (tick - 1) * 45"
              class="grid-line"
            />
            <polyline
              :points="trendPolyline"
              fill="none"
              stroke="#2563eb"
              stroke-width="4"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
            <circle
              v-for="point in trendPoints"
              :key="point.label"
              :cx="point.x"
              :cy="point.y"
              r="5"
              fill="#2563eb"
            />
            <text
              v-for="point in trendPoints"
              :key="`${point.label}-label`"
              :x="point.x"
              y="245"
              text-anchor="middle"
              class="axis-label"
            >
              {{ point.label }}
            </text>
          </svg>
        </div>

        <div class="chart-panel">
          <div class="panel-header">
            <h2>Analysis Status</h2>
            <span>{{ summary.analyzed }}/{{ summary.totalItems }}</span>
          </div>
          <div class="space-y-4">
            <div
              v-for="status in statusBreakdown"
              :key="status.label"
              class="status-row"
            >
              <div class="flex justify-between mb-1">
                <span>{{ status.label }}</span>
                <strong>{{ status.count }}</strong>
              </div>
              <div class="status-track">
                <div
                  class="status-fill"
                  :class="status.color"
                  :style="{ width: `${status.percent}%` }"
                ></div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';

const router = useRouter();

const loading = ref(false);
const usingMockData = ref(false);
const errorMsg = ref('');
const dashboardItems = ref([]);
const projectSummaries = ref([]);

const mockProjects = [
  { projectId: 1, name: 'Polymer Stability', visibility: 'PRIVATE' },
  { projectId: 2, name: 'Catalyst Screening', visibility: 'PUBLIC' },
  { projectId: 3, name: 'Battery Electrolytes', visibility: 'PRIVATE' },
  { projectId: 4, name: 'Spectra Archive', visibility: 'PUBLIC' }
];

const mockItems = [
  { id: 11, projectId: 1, projectName: 'Polymer Stability', type: 'document', status: 'COMPLETED', uploadTimestamp: daysAgo(6) },
  { id: 12, projectId: 1, projectName: 'Polymer Stability', type: 'image', status: 'COMPLETED', uploadTimestamp: daysAgo(5) },
  { id: 13, projectId: 1, projectName: 'Polymer Stability', type: 'document', status: 'PROCESSING', uploadTimestamp: daysAgo(3) },
  { id: 21, projectId: 2, projectName: 'Catalyst Screening', type: 'document', status: 'COMPLETED', uploadTimestamp: daysAgo(4) },
  { id: 22, projectId: 2, projectName: 'Catalyst Screening', type: 'image', status: 'FAILED', uploadTimestamp: daysAgo(2) },
  { id: 23, projectId: 2, projectName: 'Catalyst Screening', type: 'image', status: 'PENDING', uploadTimestamp: daysAgo(1) },
  { id: 31, projectId: 3, projectName: 'Battery Electrolytes', type: 'document', status: 'COMPLETED', uploadTimestamp: daysAgo(2) },
  { id: 32, projectId: 3, projectName: 'Battery Electrolytes', type: 'document', status: 'COMPLETED', uploadTimestamp: daysAgo(0) },
  { id: 41, projectId: 4, projectName: 'Spectra Archive', type: 'image', status: 'PROCESSING', uploadTimestamp: daysAgo(0) }
];

const summary = computed(() => {
  const totalFiles = dashboardItems.value.filter(item => item.type === 'document').length;
  const totalImages = dashboardItems.value.filter(item => item.type === 'image').length;
  const analyzed = dashboardItems.value.filter(item => item.status === 'COMPLETED').length;

  return {
    totalProjects: projectSummaries.value.length,
    totalFiles,
    totalImages,
    analyzed,
    totalItems: dashboardItems.value.length
  };
});

const projectBars = computed(() => {
  const maxCount = Math.max(...projectSummaries.value.map(item => item.count), 1);
  return projectSummaries.value
    .map(project => ({
      ...project,
      percent: Math.max((project.count / maxCount) * 100, project.count > 0 ? 8 : 0)
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 8);
});

const pieBackground = computed(() => {
  const total = Math.max(summary.value.totalFiles + summary.value.totalImages, 1);
  const filePercent = (summary.value.totalFiles / total) * 100;
  return `conic-gradient(#3b82f6 0 ${filePercent}%, #10b981 ${filePercent}% 100%)`;
});

const statusBreakdown = computed(() => {
  const statuses = [
    { label: 'Completed', key: 'COMPLETED', color: 'bg-green-500' },
    { label: 'Processing', key: 'PROCESSING', color: 'bg-blue-500' },
    { label: 'Pending', key: 'PENDING', color: 'bg-yellow-500' },
    { label: 'Failed', key: 'FAILED', color: 'bg-red-500' }
  ];
  const total = Math.max(summary.value.totalItems, 1);

  return statuses.map(status => {
    const count = dashboardItems.value.filter(item => item.status === status.key).length;
    return {
      ...status,
      count,
      percent: (count / total) * 100
    };
  });
});

const trendBuckets = computed(() => {
  const buckets = [];
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date();
    date.setDate(date.getDate() - i);
    const key = date.toISOString().slice(0, 10);
    buckets.push({
      key,
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      count: 0
    });
  }

  dashboardItems.value.forEach(item => {
    if (!item.uploadTimestamp) return;
    const key = new Date(item.uploadTimestamp).toISOString().slice(0, 10);
    const bucket = buckets.find(entry => entry.key === key);
    if (bucket) bucket.count += 1;
  });

  return buckets;
});

const trendPoints = computed(() => {
  const max = Math.max(...trendBuckets.value.map(item => item.count), 1);
  return trendBuckets.value.map((item, index) => {
    const x = 50 + index * 105;
    const y = 210 - (item.count / max) * 160;
    return { ...item, x, y };
  });
});

const trendPolyline = computed(() => trendPoints.value.map(point => `${point.x},${point.y}`).join(' '));

onMounted(() => {
  loadDashboardData();
});

async function loadDashboardData() {
  loading.value = true;
  errorMsg.value = '';
  usingMockData.value = false;

  try {
    const projectRes = await request.get('/projects');
    const projects = projectRes.code === 200 && Array.isArray(projectRes.data) ? projectRes.data : [];

    if (projects.length === 0) {
      useMockData('No backend project data yet. Showing mock dashboard data.');
      return;
    }

    const itemGroups = await Promise.all(projects.map(loadProjectItems));
    const items = itemGroups.flat();

    if (items.length === 0) {
      useMockData('Backend returned projects but no files. Showing mock dashboard data.');
      return;
    }

    dashboardItems.value = items;
    projectSummaries.value = projects.map(project => ({
      projectId: project.projectId,
      name: project.name || `Project ${project.projectId}`,
      visibility: project.visibility,
      count: items.filter(item => item.projectId === project.projectId).length
    }));
  } catch (err) {
    console.error('Dashboard data load failed:', err);
    useMockData('Backend dashboard APIs are unavailable. Showing mock dashboard data.');
  } finally {
    loading.value = false;
  }
}

async function loadProjectItems(project) {
  const projectId = project.projectId;
  const projectName = project.name || `Project ${projectId}`;
  const items = [];

  try {
    const fileRes = await request.get(`/projects/${projectId}/files`);
    if (fileRes.code === 200 && Array.isArray(fileRes.data)) {
      items.push(...fileRes.data.map(file => ({
        id: file.fileId,
        projectId,
        projectName,
        type: 'document',
        uploadTimestamp: file.uploadTimestamp,
        status: 'PENDING'
      })));
    }
  } catch (err) {
    console.error(`Load files failed for project ${projectId}:`, err);
  }

  try {
    const imageRes = await request.get(`/projects/${projectId}/images`);
    if (imageRes.code === 200 && Array.isArray(imageRes.data)) {
      items.push(...imageRes.data.map(image => ({
        id: image.imageId,
        projectId,
        projectName,
        type: 'image',
        uploadTimestamp: image.uploadTimestamp,
        status: 'PENDING'
      })));
    }
  } catch (err) {
    console.error(`Load images failed for project ${projectId}:`, err);
  }

  await Promise.all(items.map(loadItemAnalysisStatus));
  return items;
}

async function loadItemAnalysisStatus(item) {
  if (!item.id) return;

  try {
    const fileType = item.type === 'image' ? 'image' : 'file';
    const res = await request.get(`/files/${item.id}/analysis?fileType=${fileType}`);
    if (res.code === 200 && res.data?.status) {
      item.status = res.data.status;
    }
  } catch {
    item.status = 'PENDING';
  }
}

function useMockData(message) {
  usingMockData.value = true;
  errorMsg.value = message;
  dashboardItems.value = [...mockItems];
  projectSummaries.value = mockProjects.map(project => ({
    ...project,
    count: mockItems.filter(item => item.projectId === project.projectId).length
  }));
}

function daysAgo(dayCount) {
  const date = new Date();
  date.setDate(date.getDate() - dayCount);
  return date.toISOString();
}

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  router.push('/login');
}
</script>

<style scoped>
@reference "../assets/main.css";

.nav-item {
  @apply block px-4 py-2 rounded-lg hover:bg-blue-100 hover:text-blue-600 transition;
}

.router-link-exact-active,
.router-link-active {
  @apply bg-blue-600 text-white shadow-sm;
}

.metric-card {
  @apply bg-white rounded-xl shadow p-5;
}

.metric-label {
  @apply text-sm font-medium text-gray-500;
}

.metric-value {
  @apply text-4xl font-bold text-gray-900 mt-2;
}

.chart-panel {
  @apply bg-white rounded-xl shadow p-6 min-h-80;
}

.panel-header {
  @apply flex items-center justify-between mb-5;
}

.panel-header h2 {
  @apply text-xl font-bold text-gray-800;
}

.panel-header span {
  @apply text-sm text-gray-400;
}

.bar-chart {
  @apply space-y-4;
}

.bar-row {
  @apply grid grid-cols-[140px_1fr_40px] items-center gap-3;
}

.bar-name {
  @apply text-sm text-gray-600 truncate;
}

.bar-track {
  @apply h-4 bg-gray-100 rounded-full overflow-hidden;
}

.bar-fill {
  @apply h-full bg-blue-600 rounded-full transition-all;
}

.bar-count {
  @apply text-right text-sm font-semibold text-gray-700;
}

.pie-wrap {
  @apply flex flex-col items-center justify-center gap-6 h-56;
}

.pie-chart {
  width: 150px;
  height: 150px;
  border-radius: 9999px;
  box-shadow: inset 0 0 0 18px rgba(255, 255, 255, 0.65);
}

.legend {
  @apply grid gap-2 text-sm text-gray-600;
}

.legend-dot {
  @apply inline-block w-3 h-3 rounded-full mr-2;
}

.line-chart {
  @apply w-full h-64;
}

.grid-line {
  stroke: #e5e7eb;
  stroke-width: 1;
}

.axis-label {
  fill: #6b7280;
  font-size: 13px;
}

.status-row {
  @apply text-sm text-gray-700;
}

.status-track {
  @apply h-3 bg-gray-100 rounded-full overflow-hidden;
}

.status-fill {
  @apply h-full rounded-full transition-all;
}
</style>
