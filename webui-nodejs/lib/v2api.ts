import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "/api",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
});

// ── Dashboard ──────────────────────────────────────────────────────────
export async function fetchTopics(params: {
  page?: number;
  size?: number;
  categoryId?: number;
  sourceType?: string;
  dateFrom?: string;
  dateTo?: string;
  minQuality?: number;
}) {
  const { data } = await api.get("/topics", { params });
  return data as {
    topics: Record<string, unknown>[];
    total: number;
    page: number;
    size: number;
  };
}

export async function fetchStats() {
  const { data } = await api.get("/stats");
  return data as {
    totalTopics: number;
    topicsThisWeek: number;
    pendingJobs: number;
    failedJobs: number;
    byCategory: Record<string, unknown>[];
    bySource: Record<string, unknown>[];
  };
}

// ── Topic Detail ───────────────────────────────────────────────────────
export async function fetchTopicDetail(id: number) {
  const { data } = await api.get(`/topics/${id}`);
  return data as Record<string, unknown>;
}

// ── Categories ─────────────────────────────────────────────────────────
export async function fetchCategories() {
  const { data } = await api.get("/categories");
  return data as Record<string, unknown>[];
}

export async function createCategory(body: Record<string, unknown>) {
  const { data } = await api.post("/categories", body);
  return data;
}

export async function updateCategory(
  id: number,
  body: Record<string, unknown>
) {
  const { data } = await api.put(`/categories/${id}`, body);
  return data;
}

export async function deleteCategory(id: number) {
  await api.delete(`/categories/${id}`);
}

export async function fetchScanners() {
  const { data } = await api.get("/scanners");
  return data as { type: string; displayName: string }[];
}

// ── Content Studio ─────────────────────────────────────────────────────
export async function fetchUserContent() {
  const { data } = await api.get("/content");
  return data as Record<string, unknown>[];
}

export async function uploadContent(body: {
  title: string;
  content: string;
  contentType: string;
}) {
  const { data } = await api.post("/content", body);
  return data;
}

export async function importContentFromUrl(url: string) {
  const { data } = await api.post("/content/import", { url });
  return data;
}

export async function deleteUserContent(id: number) {
  await api.delete(`/content/${id}`);
}

export async function analyzeStyle(contentId: number) {
  const { data } = await api.post(`/content/${contentId}/analyze`);
  return data;
}

export async function fetchStyleProfile() {
  const { data } = await api.get("/content/style-profile");
  return data;
}

export async function generateContent(body: {
  topicId?: number;
  groupId?: string;
  outputFormat: string;
}) {
  const { data } = await api.post("/generate", body);
  return data;
}

export async function fetchGeneratedContent() {
  const { data } = await api.get("/generate");
  return data as Record<string, unknown>[];
}

export async function fetchGeneratedContentDetail(id: number) {
  const { data } = await api.get(`/generate/${id}`);
  return data as Record<string, unknown>;
}

export async function regenerateContent(id: number, feedback: string) {
  const { data } = await api.post(`/generate/${id}/regenerate`, { feedback });
  return data;
}

export async function exportMarkdown(id: number) {
  const { data } = await api.get(`/generate/${id}/export`);
  return data as string;
}

// ── Pipeline Monitor ───────────────────────────────────────────────────
export async function fetchPipelineStatus() {
  const { data } = await api.get("/pipeline/status");
  return data as {
    jobCounts: Record<string, unknown>[];
    summary: { pending: number; processing: number; completed: number; failed: number };
  };
}

export async function fetchPipelineErrors(limit = 20) {
  const { data } = await api.get("/pipeline/errors", { params: { limit } });
  return data as Record<string, unknown>[];
}

export async function fetchScanHistory() {
  const { data } = await api.get("/pipeline/scan-history");
  return data as Record<string, unknown>[];
}

export async function triggerScan() {
  const { data } = await api.post("/pipeline/scan");
  return data as { triggered: boolean; message: string };
}

export async function fetchTopicsByStage() {
  const { data } = await api.get("/pipeline/topics-by-stage");
  return data as Record<string, unknown>[];
}
