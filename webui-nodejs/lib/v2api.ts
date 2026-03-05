import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "/api",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
});

// ── Shared types ───────────────────────────────────────────────────────

export interface TopicSummary {
  id: number;
  title: string;
  url?: string;
  source_type?: string;
  source_date?: string;
  collected_at?: string;
  pipeline_stage?: string;
  quality_score?: number;
  relevance_score?: number;
  summary?: string;
  tags?: string;
  category_id?: number;
}

export interface TopicDetail extends TopicSummary {
  extracted_content?: string;
  group_id?: string;
  is_rejected?: boolean;
  rejection_reason?: string;
  relatedTopics?: TopicSummary[];
}

export interface TopicsPage {
  topics: TopicSummary[];
  total: number;
  page: number;
  size: number;
}

export interface DashboardStats {
  totalTopics: number;
  topicsThisWeek: number;
  pendingJobs: number;
  failedJobs: number;
  byCategory: { id?: number; name: string; topic_count: number }[];
  bySource: { name: string; topic_count: number }[];
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  keywords?: string;
  negative_keywords?: string;
  source_types?: string;
  scan_interval_minutes?: number;
  topic_count?: number;
  last_scan_time?: string;
}

export interface CategoryInput {
  name: string;
  description?: string;
  keywords?: string;
  negative_keywords?: string;
  source_types?: string;
  scan_interval_minutes?: number;
}

export interface ScannerInfo {
  type: string;
  displayName: string;
}

export interface UserContent {
  id: number;
  title: string;
  content?: string;
  content_type?: string;
  style_analyzed?: boolean;
  writing_style_metadata?: string;
  created_at?: string;
}

export interface GeneratedContent {
  id: number;
  topic_id?: number;
  group_id?: string;
  output_format?: string;
  generated_text?: string;
  model_used?: string;
  feedback?: string;
  created_at?: string;
}

export interface PipelineJobCount {
  stage: string;
  status: string;
  count: number;
}

export interface PipelineSummary {
  pending: number;
  processing: number;
  completed: number;
  failed: number;
}

export interface PipelineError {
  id?: number;
  topic_id?: number;
  stage?: string;
  error_message?: string;
  updated_at?: string;
}

export interface ScanHistoryEntry {
  category_id?: number;
  category_name?: string;
  source_type?: string;
  topics_found?: number;
  new_topics?: number;
  started_at?: string;
  status?: string;
}

export interface TopicStageCount {
  pipeline_stage: string;
  count: number;
}

// ── Dashboard ──────────────────────────────────────────────────────────
export async function fetchTopics(params: {
  page?: number;
  size?: number;
  categoryId?: number;
  sourceType?: string;
  dateFrom?: string;
  dateTo?: string;
  minQuality?: number;
}): Promise<TopicsPage> {
  const { data } = await api.get("/topics", { params });
  return data;
}

export async function fetchStats(): Promise<DashboardStats> {
  const { data } = await api.get("/stats");
  return data;
}

// ── Topic Detail ───────────────────────────────────────────────────────
export async function fetchTopicDetail(id: number): Promise<TopicDetail> {
  const { data } = await api.get(`/topics/${id}`);
  return data;
}

// ── Categories ─────────────────────────────────────────────────────────
export async function fetchCategories(): Promise<Category[]> {
  const { data } = await api.get("/categories");
  return data;
}

export async function createCategory(body: CategoryInput) {
  const { data } = await api.post("/categories", body);
  return data as { id: number };
}

export async function updateCategory(id: number, body: CategoryInput) {
  const { data } = await api.put(`/categories/${id}`, body);
  return data;
}

export async function deleteCategory(id: number) {
  await api.delete(`/categories/${id}`);
}

export async function fetchScanners(): Promise<ScannerInfo[]> {
  const { data } = await api.get("/scanners");
  return data;
}

// ── Content Studio ─────────────────────────────────────────────────────
export async function fetchUserContent(): Promise<UserContent[]> {
  const { data } = await api.get("/content");
  return data;
}

export async function uploadContent(body: {
  title: string;
  content: string;
  contentType: string;
}) {
  const { data } = await api.post("/content", body);
  return data as { id: number };
}

export async function importContentFromUrl(url: string) {
  const { data } = await api.post("/content/import", { url });
  return data as { id: number };
}

export async function deleteUserContent(id: number) {
  await api.delete(`/content/${id}`);
}

export async function analyzeStyle(contentId: number) {
  const { data } = await api.post(`/content/${contentId}/analyze`);
  return data;
}

export async function fetchStyleProfile(): Promise<Record<string, string> | null> {
  const { data } = await api.get("/content/style-profile");
  return data;
}

export async function generateContent(body: {
  topicId?: number;
  groupId?: string;
  outputFormat: string;
}) {
  const { data } = await api.post("/generate", body);
  return data as { id: number };
}

export async function fetchGeneratedContent(): Promise<GeneratedContent[]> {
  const { data } = await api.get("/generate");
  return data;
}

export async function fetchGeneratedContentDetail(id: number): Promise<GeneratedContent> {
  const { data } = await api.get(`/generate/${id}`);
  return data;
}

export async function regenerateContent(id: number, feedback: string) {
  const { data } = await api.post(`/generate/${id}/regenerate`, { feedback });
  return data as { id: number };
}

export async function exportMarkdown(id: number): Promise<string> {
  const { data } = await api.get(`/generate/${id}/export`);
  return data;
}

// ── Pipeline Monitor ───────────────────────────────────────────────────
export async function fetchPipelineStatus(): Promise<{
  jobCounts: PipelineJobCount[];
  summary: PipelineSummary;
}> {
  const { data } = await api.get("/pipeline/status");
  return data;
}

export async function fetchPipelineErrors(limit = 20): Promise<PipelineError[]> {
  const { data } = await api.get("/pipeline/errors", { params: { limit } });
  return data;
}

export async function fetchScanHistory(): Promise<ScanHistoryEntry[]> {
  const { data } = await api.get("/pipeline/scan-history");
  return data;
}

export async function triggerScan() {
  const { data } = await api.post("/pipeline/scan");
  return data as { triggered: boolean; message: string };
}

export async function fetchTopicsByStage(): Promise<TopicStageCount[]> {
  const { data } = await api.get("/pipeline/topics-by-stage");
  return data;
}
