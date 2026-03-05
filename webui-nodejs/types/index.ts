import { createContext, useContext, ReactNode } from 'react'

// API Configuration
export interface ApiConfig {
  baseUrl: string
  timeout: number
  retries: number
}

// Theme Types
export interface Theme {
  id: number
  name: string
  description: string
  topicCount: number
  createdAt: string
  updatedAt: string
  parentTheme?: Theme
  subThemes?: Theme[]
  fullPath?: string  // e.g., "Kubernetes/Networking"
  keywords?: string[]  // Search keywords for this theme
  searchKeywords?: string  // JSON string of keywords (backend format)
  enabled?: boolean  // Whether this theme is enabled for topic scanning
}

// Topic Types
export interface Topic {
  id: number
  title: string
  content: string
  contentSummary?: string  // LLM-generated summary of the content
  conversationSummary?: string  // LLM-generated summary of the conversation thread
  url: string
  source?: Source
  author: string
  publishedAt: string
  score: number
  viewCount: number
  interactionCount: number
  engagementScore?: number
  themes: Theme[]
  primaryTheme?: Theme
  metadata: Record<string, any>
  createdAt: string
  updatedAt: string
  thumbsUp?: number
  thumbsDown?: number
  isRejected?: boolean
  rejectionReason?: string
  tags?: string[]
  tagsList?: string[]
  topicGroupId?: number
  contentExtractionStatus?: string
  contentExtractionCompletedAt?: string
  technicalQualityScore?: number
  marketingScore?: number
  isHotTopic?: boolean
}

// Search Topic Types
export interface SearchTopic {
  id: number
  keyword: string
  searchQuery: string
  description: string
  isActive: boolean
  priority: number
  maxResults: number
  searchFrequencyHours: number
  lastSearchedAt: string
  createdAt: string
  updatedAt: string
}

// Source Types
export interface Source {
  id?: number
  name: string
  description?: string
  url?: string
  baseUrl?: string  // Backend uses baseUrl
  apiEndpoint?: string
  isActive?: boolean
  scanFrequencyHours?: number
  lastScanAt?: string
  createdAt?: string
  updatedAt?: string
}

// Personal Content Types
export interface PersonalContent {
  id: number
  title: string
  content: string
  contentType: string  // GENERAL, BLOG, VIDEO_SCRIPT, ARTICLE, etc.
  category?: string
  tags?: string  // JSON string
  tagsList?: string[]  // Parsed tags array
  userId?: string
  writingStyleMetadata?: string  // JSON string
  ragStored: boolean
  ragStoredAt?: string
  createdAt: string
  updatedAt: string
}

// Content Generation Types
export interface ContentGenerationRequest {
  prompt: string
  contentType: 'YOUTUBE_STORYBOARD' | 'BLOG_POST' | 'ARTICLE'
  category?: string
  tags?: string[]
  usePersonalStyle?: boolean
}

export interface ContentGenerationResponse {
  generatedContent: string
  contentType: string
  metadata?: {
    estimatedDuration?: number  // For YouTube
    wordCount?: number
    sections?: string[]
  }
}

// Scan History Types
export interface ScanHistory {
  id: number
  sourceId: number
  sourceName: string
  startedAt: string
  completedAt: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED'
  topicsFound: number
  errorMessage: string
  duration: number
}

// Dashboard Stats
export interface DashboardStats {
  totalTopics: number
  totalThemes: number
  totalSources: number
  activeSources: number
  topicsToday: number
  newTopicsToday: number
  topicsThisWeek: number
  topThemes: Array<{
    theme: Theme
    count: number
  }>
  recentActivity: Array<{
    type: 'topic' | 'scan' | 'theme'
    message: string
    timestamp: string
  }>
}

// API Response Types
export interface ApiResponse<T> {
  data: T
  success: boolean
  message?: string
  error?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  data?: T[] // Legacy support
  total: number
  totalElements?: number
  page: number
  size: number
  totalPages: number
  number?: number // Spring Data page number
}

// Form Types
export interface CreateThemeForm {
  name: string
  description: string
}

export interface CreateSourceForm {
  name: string
  description: string
  url: string
}

export interface CreateSearchTopicForm {
  keyword: string
  searchQuery: string
  description: string
  priority: number
  maxResults: number
  searchFrequencyHours: number
}

// Filter Types
export interface TopicFilters {
  themeId?: number
  sourceId?: number
  dateFrom?: string
  dateTo?: string
  minScore?: number
  search?: string
}

export interface SortOptions {
  field: 'publishedAt' | 'score' | 'viewCount' | 'title' | 'engagementScore' | 'interactionCount'
  direction: 'asc' | 'desc'
}

// Chart Data Types
export interface ChartDataPoint {
  name: string
  value: number
  color?: string
}

export interface TimeSeriesDataPoint {
  date: string
  value: number
  label?: string
}

// Notification Types
export interface Notification {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  title: string
  message: string
  timestamp: string
  read: boolean
}

// Context Types
export interface AppContextType {
  user: any // Define user type based on auth implementation
  theme: 'light' | 'dark'
  notifications: Notification[]
  addNotification: (notification: Omit<Notification, 'id' | 'timestamp'>) => void
  removeNotification: (id: string) => void
  toggleTheme: () => void
}

// Hook Types
export interface UseApiOptions {
  enabled?: boolean
  refetchInterval?: number
  onSuccess?: (data: any) => void
  onError?: (error: any) => void
}

// Component Props Types
export interface TableColumn<T> {
  key: keyof T
  label: string
  sortable?: boolean
  render?: (value: any, item: T) => ReactNode
}

export interface TableProps<T> {
  data: T[]
  columns: TableColumn<T>[]
  loading?: boolean
  pagination?: {
    page: number
    size: number
    total: number
    onPageChange: (page: number) => void
    onSizeChange: (size: number) => void
  }
  sorting?: {
    field: keyof T
    direction: 'asc' | 'desc'
    onSort: (field: keyof T) => void
  }
  selection?: {
    selected: T[]
    onSelectionChange: (selected: T[]) => void
  }
}

export interface ChartProps {
  data: ChartDataPoint[]
  type: 'bar' | 'line' | 'pie' | 'area'
  title?: string
  height?: number
  showLegend?: boolean
  showTooltip?: boolean
}

// Error Types
export interface ApiError {
  message: string
  code: string
  status: number
  details?: any
}

// Configuration Types
export interface AppConfig {
  api: ApiConfig
  features: {
    realTimeUpdates: boolean
    darkMode: boolean
    analytics: boolean
    notifications: boolean
  }
  ui: {
    defaultPageSize: number
    maxPageSize: number
    refreshInterval: number
    animationDuration: number
  }
}
