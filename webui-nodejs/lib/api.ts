import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { injectTraceContext } from './tracing'
import { 
  Theme, 
  Topic, 
  Source, 
  SearchTopic, 
  ScanHistory, 
  DashboardStats,
  ApiResponse,
  PaginatedResponse,
  TopicFilters,
  SortOptions,
  PersonalContent,
  ContentGenerationRequest,
  ContentGenerationResponse
} from '../types'

class ApiClient {
  private client: AxiosInstance

  constructor(baseURL?: string) {
    // Use relative URLs in browser (Next.js will proxy via rewrites)
    // Only use absolute URL if explicitly provided (e.g., for server-side rendering)
    const apiBaseURL = baseURL || process.env.NEXT_PUBLIC_API_URL || ''
    this.client = axios.create({
      baseURL: apiBaseURL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    })

    // Request interceptor
    this.client.interceptors.request.use(
      (config) => {
        // Add auth token if available
        const token = localStorage.getItem('authToken')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        
        // Inject OpenTelemetry trace context (traceparent header)
        // This ensures all API calls are part of the same trace
        if (typeof window !== 'undefined') {
          injectTraceContext(config.headers as Record<string, string>)
        }
        
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor
    this.client.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          // Handle unauthorized access
          localStorage.removeItem('authToken')
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
    )
  }

  // Themes API
  async getThemes(includeDisabled: boolean = false): Promise<Theme[]> {
    const params = includeDisabled ? '?includeDisabled=true' : ''
    const response: AxiosResponse<Theme[]> = await this.client.get(`/api/themes${params}`)
    return response.data
  }

  async getTheme(id: number): Promise<Theme> {
    const response: AxiosResponse<Theme> = await this.client.get(`/api/themes/${id}`)
    return response.data
  }

  async createTheme(theme: Partial<Theme>): Promise<Theme> {
    const response: AxiosResponse<Theme> = await this.client.post('/api/themes', theme)
    return response.data
  }

  async updateTheme(id: number, theme: Partial<Theme>): Promise<Theme> {
    const response: AxiosResponse<Theme> = await this.client.put(`/api/themes/${id}`, theme)
    return response.data
  }

  async toggleThemeEnabled(id: number, enabled: boolean): Promise<Theme> {
    // Use the existing updateTheme endpoint which already handles the enabled field
    return this.updateTheme(id, { enabled })
  }

  async deleteTheme(id: number): Promise<void> {
    await this.client.delete(`/api/themes/${id}`)
  }

  async regenerateThemeKeywords(id: number): Promise<{ theme: Theme; keywords: string[]; message: string }> {
    const response: AxiosResponse<{ theme: Theme; keywords: string[]; message: string }> = 
      await this.client.post(`/api/themes/${id}/regenerate-keywords`)
    return response.data
  }

  // Topics API
  async getTopics(filters?: TopicFilters, sort?: SortOptions, page = 0, size = 20): Promise<PaginatedResponse<Topic>> {
    const params = new URLSearchParams()
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, value.toString())
        }
      })
    }
    
    if (sort) {
      params.append('sort', `${sort.field},${sort.direction}`)
    }
    
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response: AxiosResponse<PaginatedResponse<Topic>> = await this.client.get(`/api/topics?${params}`)
    return response.data
  }

  async getTopic(id: number): Promise<Topic> {
    const response: AxiosResponse<Topic> = await this.client.get(`/api/topics/${id}`)
    return response.data
  }

  async getTopicsByTheme(themeId: number, page = 0, size = 20): Promise<PaginatedResponse<Topic>> {
    const response: AxiosResponse<PaginatedResponse<Topic>> = await this.client.get(
      `/api/themes/${themeId}/topics?page=${page}&size=${size}`
    )
    return response.data
  }

  async getRecentTopics(limit = 10): Promise<Topic[]> {
    const response: AxiosResponse<Topic[]> = await this.client.get(`/api/topics/recent?limit=${limit}`)
    return response.data
  }

  async getPopularTopics(limit = 10): Promise<Topic[]> {
    const response: AxiosResponse<Topic[]> = await this.client.get(`/api/topics/popular?limit=${limit}`)
    return response.data
  }

  async getHottestTopics(limit = 20): Promise<Topic[]> {
    const response: AxiosResponse<Topic[]> = await this.client.get(`/api/topics/hottest?limit=${limit}`)
    return response.data
  }

  async getRelatedTopics(topicId: number): Promise<Topic[]> {
    const response: AxiosResponse<Topic[]> = await this.client.get(`/api/topics/${topicId}/related`)
    return response.data
  }

  async thumbsUpTopic(topicId: number): Promise<{ success: boolean; message: string }> {
    const response: AxiosResponse<{ success: boolean; message: string }> = 
      await this.client.post(`/api/topics/${topicId}/thumbs-up`)
    return response.data
  }

  async thumbsDownTopic(topicId: number, reason?: string): Promise<{ success: boolean; message: string }> {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : ''
    const response: AxiosResponse<{ success: boolean; message: string }> = 
      await this.client.post(`/api/topics/${topicId}/thumbs-down${params}`)
    return response.data
  }

  async updateTopicTags(topicId: number, tags: string[]): Promise<{ success: boolean; tags: string[]; message: string }> {
    const response: AxiosResponse<{ success: boolean; tags: string[]; message: string }> = 
      await this.client.put(`/api/topics/${topicId}/tags`, { tags })
    return response.data
  }

  async regenerateTopicTags(topicId: number): Promise<{ success: boolean; tags: string[]; message: string }> {
    const response: AxiosResponse<{ success: boolean; tags: string[]; message: string }> = 
      await this.client.post(`/api/topics/${topicId}/tags/regenerate`)
    return response.data
  }

  // Clusters API
  async getClusters(page = 0, size = 20, themeId?: number): Promise<PaginatedResponse<any>> {
    const params = new URLSearchParams()
    params.append('page', page.toString())
    params.append('size', size.toString())
    if (themeId) {
      params.append('themeId', themeId.toString())
    }
    // Use a longer timeout for clusters (20 seconds) since filtering by theme can take time
    const response: AxiosResponse<PaginatedResponse<any>> = 
      await this.client.get(`/api/topics/clusters?${params}`, {
        timeout: 20000 // 20 seconds for cluster listing
      })
    return response.data
  }

  async getClusterDetails(groupId: number): Promise<{
    groupId: number
    representativeTopic: { id: number; title: string; url: string } | null
    topicCount: number
    summary: string | null
    clusterName: string | null
    tags: string[]
    topics: Topic[]
  }> {
    // Use a longer timeout for cluster details (30 seconds) since summary generation can take time
    const response: AxiosResponse<{
      groupId: number
      representativeTopic: { id: number; title: string; url: string } | null
      topicCount: number
      summary: string | null
      clusterName: string | null
      tags: string[]
      topics: Topic[]
    }> = await this.client.get(`/api/topics/cluster/${groupId}`, {
      timeout: 30000 // 30 seconds for cluster detail with summary generation
    })
    return response.data
  }

  // Sub-theme API
  async getTopLevelThemes(): Promise<Theme[]> {
    const response: AxiosResponse<Theme[]> = await this.client.get('/api/themes/top-level')
    return response.data
  }

  async getSubThemes(parentId: number): Promise<Theme[]> {
    const response: AxiosResponse<Theme[]> = await this.client.get(`/api/themes/${parentId}/sub-themes`)
    return response.data
  }

  async getTopicsBySubTheme(subThemeId: number, page = 0, size = 20): Promise<PaginatedResponse<Topic>> {
    const response: AxiosResponse<PaginatedResponse<Topic>> = await this.client.get(
      `/api/topics/subtheme/${subThemeId}?page=${page}&size=${size}`
    )
    return response.data
  }

  async getHottestTopicsBySubTheme(subThemeId: number, limit = 20): Promise<Topic[]> {
    const response: AxiosResponse<Topic[]> = await this.client.get(
      `/api/topics/subtheme/${subThemeId}/hottest?limit=${limit}`
    )
    return response.data
  }

  // Sources API
  async getSources(): Promise<Source[]> {
    const response: AxiosResponse<Source[]> = await this.client.get('/api/sources')
    return response.data
  }

  async getSource(id: number): Promise<Source> {
    const response: AxiosResponse<Source> = await this.client.get(`/api/sources/${id}`)
    return response.data
  }

  async createSource(source: Partial<Source>): Promise<Source> {
    const response: AxiosResponse<Source> = await this.client.post('/api/sources', source)
    return response.data
  }

  async updateSource(id: number, source: Partial<Source>): Promise<Source> {
    const response: AxiosResponse<Source> = await this.client.put(`/api/sources/${id}`, source)
    return response.data
  }

  async deleteSource(id: number): Promise<void> {
    await this.client.delete(`/api/sources/${id}`)
  }

  async scanSource(id: number): Promise<void> {
    await this.client.post(`/api/sources/${id}/scan`)
  }

  // Search Topics API
  async getSearchTopics(sourceId?: number): Promise<SearchTopic[]> {
    const url = sourceId ? `/api/search-topics?sourceId=${sourceId}` : '/api/search-topics'
    const response: AxiosResponse<SearchTopic[]> = await this.client.get(url)
    return response.data
  }

  async getSearchTopic(id: number): Promise<SearchTopic> {
    const response: AxiosResponse<SearchTopic> = await this.client.get(`/api/search-topics/${id}`)
    return response.data
  }

  async createSearchTopic(searchTopic: Partial<SearchTopic>): Promise<SearchTopic> {
    const response: AxiosResponse<SearchTopic> = await this.client.post('/api/search-topics', searchTopic)
    return response.data
  }

  async updateSearchTopic(id: number, searchTopic: Partial<SearchTopic>): Promise<SearchTopic> {
    const response: AxiosResponse<SearchTopic> = await this.client.put(`/api/search-topics/${id}`, searchTopic)
    return response.data
  }

  async deleteSearchTopic(id: number): Promise<void> {
    await this.client.delete(`/api/search-topics/${id}`)
  }

  async toggleSearchTopicStatus(id: number): Promise<SearchTopic> {
    const response: AxiosResponse<SearchTopic> = await this.client.patch(`/api/search-topics/${id}/toggle`)
    return response.data
  }

  // Scan History API
  async getScanHistory(sourceId?: number, limit = 50): Promise<ScanHistory[]> {
    const url = sourceId ? `/api/scan-history?sourceId=${sourceId}&limit=${limit}` : `/api/scan-history?limit=${limit}`
    const response: AxiosResponse<ScanHistory[]> = await this.client.get(url)
    return response.data
  }

  async getScanHistoryById(id: number): Promise<ScanHistory> {
    const response: AxiosResponse<ScanHistory> = await this.client.get(`/api/scan-history/${id}`)
    return response.data
  }

  // Dashboard API
  async getDashboardStats(): Promise<DashboardStats> {
    const response: AxiosResponse<DashboardStats> = await this.client.get('/api/dashboard/stats')
    return response.data
  }

  async getThemeStats(): Promise<any> {
    const response: AxiosResponse<any> = await this.client.get('/api/dashboard/theme-stats')
    return response.data
  }

  async getMonitoringMetrics(): Promise<any> {
    const response: AxiosResponse<any> = await this.client.get('/api/dashboard/monitoring')
    return response.data
  }

  // Monitoring API
  async getHealthStatus(): Promise<any> {
    const response: AxiosResponse<any> = await this.client.get('/api/health')
    return response.data
  }

  async getMetrics(): Promise<any> {
    const response: AxiosResponse<any> = await this.client.get('/api/metrics')
    return response.data
  }

  // Prompt Management API
  async getPromptTemplates(): Promise<any[]> {
    const response: AxiosResponse<any[]> = await this.client.get('/api/prompts/templates')
    return response.data
  }

  async getDefaultPromptTemplates(): Promise<any[]> {
    const response: AxiosResponse<any[]> = await this.client.get('/api/prompts/default-templates')
    return response.data
  }

  async testPromptTemplate(templateName: string, variables: Record<string, any>): Promise<any> {
    const response: AxiosResponse<any> = await this.client.post(`/api/prompts/templates/${templateName}/test`, variables)
    return response.data
  }

  // Semantic search for topics
  async searchTopics(
    query: string, 
    page: number = 0, 
    size: number = 20,
    minSimilarity: number = 0.5
  ): Promise<PaginatedResponse<Topic> & { query: string; topicsWithScores?: Array<{ topic: Topic; relevanceScore: number }> }> {
    const params = new URLSearchParams({
      q: query,
      page: page.toString(),
      size: size.toString(),
      minSimilarity: minSimilarity.toString()
    })

    const response: AxiosResponse<PaginatedResponse<Topic> & { query: string; topicsWithScores?: Array<{ topic: Topic; relevanceScore: number }> }> = 
      await this.client.get(`/api/topics/search?${params}`)
    return response.data
  }

  async exportTopics(format: 'csv' | 'json' | 'xlsx', filters?: TopicFilters): Promise<Blob> {
    const params = new URLSearchParams({ format })
    
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          params.append(key, value.toString())
        }
      })
    }

    const response: AxiosResponse<Blob> = await this.client.get(`/api/export?${params}`, {
      responseType: 'blob'
    })
    return response.data
  }

  // Personal Content API
  async getPersonalContent(filters?: {
    category?: string
    contentType?: string
    tag?: string
    userId?: string
  }): Promise<{ content: PersonalContent[]; total: number }> {
    const params = new URLSearchParams()
    if (filters) {
      Object.entries(filters).forEach(([key, value]) => {
        // Only add non-empty values to avoid filtering by empty strings
        if (value !== undefined && value !== null && value.toString().trim() !== '') {
          params.append(key, value.toString().trim())
        }
      })
    }
    const url = params.toString() ? `/api/personal-content?${params}` : '/api/personal-content'
    const response: AxiosResponse<{ content: PersonalContent[]; total: number }> = 
      await this.client.get(url)
    return response.data
  }

  async getPersonalContentById(id: number): Promise<PersonalContent> {
    const response: AxiosResponse<PersonalContent> = 
      await this.client.get(`/api/personal-content/${id}`)
    return response.data
  }

  async createPersonalContent(content: Partial<PersonalContent>): Promise<{ message: string; content: PersonalContent }> {
    const response: AxiosResponse<{ message: string; content: PersonalContent }> = 
      await this.client.post('/api/personal-content', content)
    return response.data
  }

  async updatePersonalContent(id: number, content: Partial<PersonalContent>): Promise<{ message: string; content: PersonalContent }> {
    const response: AxiosResponse<{ message: string; content: PersonalContent }> = 
      await this.client.put(`/api/personal-content/${id}`, content)
    return response.data
  }

  async deletePersonalContent(id: number): Promise<{ message: string }> {
    const response: AxiosResponse<{ message: string }> = 
      await this.client.delete(`/api/personal-content/${id}`)
    return response.data
  }

  async processUnprocessedContent(): Promise<{ message: string; processed: number }> {
    const response: AxiosResponse<{ message: string; processed: number }> = 
      await this.client.post('/api/personal-content/process')
    return response.data
  }

  async reprocessContent(id: number): Promise<{ message: string }> {
    const response: AxiosResponse<{ message: string }> = 
      await this.client.post(`/api/personal-content/${id}/reprocess`)
    return response.data
  }

  // Content Generation API (non-streaming, for backward compatibility)
  async generateContent(request: ContentGenerationRequest): Promise<ContentGenerationResponse> {
    // Content generation can take 60+ seconds due to LLM processing (RAG retrieval + style analysis + LLM generation)
    // Use a longer timeout (90 seconds) to accommodate the full generation process
    const response: AxiosResponse<ContentGenerationResponse> = 
      await this.client.post('/api/content/generate', request, {
        timeout: 90000 // 90 seconds for content generation
      })
    return response.data
  }

  // Content Generation API with streaming support (Server-Sent Events)
  // Uses fetch API with streaming since EventSource doesn't support POST
  async generateContentStream(
    request: ContentGenerationRequest,
    onChunk: (chunk: string) => void,
    onComplete: (response: ContentGenerationResponse) => void,
    onError: (error: string) => void
  ): Promise<void> {
    return new Promise(async (resolve, reject) => {
      try {
        // Don't use AbortController timeout - let the stream run as long as events are coming
        // The timeout should only apply if NO events are received, which we check later
        const response = await fetch('/api/content/generate/stream', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(request)
        })

        if (!response.ok) {
          // If streaming endpoint returns error, fallback to non-streaming
          console.warn(`Streaming endpoint returned status ${response.status}, falling back to non-streaming`)
          try {
            const fallbackResponse = await this.generateContent(request)
            onComplete(fallbackResponse)
            resolve()
            return
          } catch (fallbackErr: any) {
            const errorData = await response.json().catch(() => ({ message: 'Request failed' }))
            onError(errorData.message || 'Content generation failed')
            reject(new Error(errorData.message || 'Content generation failed'))
            return
          }
        }

        if (!response.body) {
          // No response body, fallback to non-streaming
          console.warn('No response body from streaming endpoint, falling back to non-streaming')
          try {
            const fallbackResponse = await this.generateContent(request)
            onComplete(fallbackResponse)
            resolve()
            return
          } catch (fallbackErr: any) {
            onError('No response body received and fallback failed')
            reject(new Error('No response body received'))
            return
          }
        }

        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let fullContent = ''
        let contentType = request.contentType || 'BLOG_POST'
        let metadata: any = {}
        let lastEventName = ''
        let eventsReceived: string[] = []
        let lastChunkTime = Date.now()
        
        // Set up a timeout that only triggers if NO events are received for 30 seconds
        // This allows the stream to continue as long as data is flowing
        let inactivityTimeout: NodeJS.Timeout | null = setTimeout(() => {
          if (eventsReceived.length === 0 || fullContent.length === 0) {
            console.warn('Stream inactive for 30 seconds with no content, aborting')
            reader.cancel()
          }
        }, 30000) // 30 seconds of inactivity

        while (true) {
          const { done, value } = await reader.read()
          
          if (done) {
            if (inactivityTimeout) {
              clearTimeout(inactivityTimeout)
            }
            break
          }
          
          // Reset inactivity timeout when we receive data
          lastChunkTime = Date.now()
          if (inactivityTimeout) {
            clearTimeout(inactivityTimeout)
          }
          inactivityTimeout = setTimeout(() => {
            if (eventsReceived.length === 0 || fullContent.length === 0) {
              console.warn('Stream inactive for 30 seconds with no content, aborting')
              reader.cancel()
            }
          }, 30000)

          buffer += decoder.decode(value, { stream: true })
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // Keep incomplete line in buffer

          for (const line of lines) {
            if (line.trim() === '') continue
            
            // Parse SSE format: "event: name\ndata: {...}\n\n"
            if (line.startsWith('event: ')) {
              lastEventName = line.substring(7).trim()
              if (lastEventName && !eventsReceived.includes(lastEventName)) {
                eventsReceived.push(lastEventName)
              }
              continue
            }
            
            if (line.startsWith('data: ')) {
              const dataStr = line.substring(6).trim()
              try {
                const data = JSON.parse(dataStr)
                
                if (data.status === 'started') {
                  contentType = data.contentType || contentType
                  console.log('Stream started for:', contentType)
                } else if (data.status === 'retrieving_context' || data.status === 'generating') {
                  console.log('Stream status:', data.status)
                } else if (data.chunk) {
                  fullContent += data.chunk
                  onChunk(data.chunk)
                } else if (data.status === 'completed') {
                  const response: ContentGenerationResponse = {
                    generatedContent: data.generatedContent || fullContent,
                    contentType: data.contentType || contentType,
                    metadata: data.metadata || metadata
                  }
                  onComplete(response)
                  resolve()
                  return
                } else if (data.error) {
                  const errorMsg = data.message || data.error || 'Content generation failed'
                  console.error('Stream error received:', errorMsg, 'Events received:', eventsReceived)
                  onError(errorMsg)
                  reject(new Error(errorMsg))
                  return
                }
              } catch (err) {
                console.error('Error parsing SSE data:', err, 'Line:', dataStr, 'Last event:', lastEventName)
              }
            }
          }
        }

        // If we reach here without completion, something went wrong
        console.warn('Stream ended. Events received:', eventsReceived, 'Content length:', fullContent.length)
        if (fullContent) {
          const response: ContentGenerationResponse = {
            generatedContent: fullContent,
            contentType,
            metadata
          }
          onComplete(response)
          resolve()
        } else {
          // Fallback to non-streaming endpoint if streaming failed
          console.warn('Streaming failed with no content, falling back to non-streaming endpoint')
          try {
            const fallbackResponse = await this.generateContent(request)
            onComplete(fallbackResponse)
            resolve()
          } catch (fallbackErr: any) {
            const errorMsg = `Stream ended without content and fallback also failed. Events received: ${eventsReceived.join(', ')}. Fallback error: ${fallbackErr.message}`
            console.error(errorMsg)
            onError(errorMsg)
            reject(new Error(errorMsg))
          }
        }
      } catch (err: any) {
        // If it's a network error (but not abort from timeout), try fallback
        // Note: We removed the timeout, so AbortError shouldn't occur unless browser cancels
        if (err.name === 'TypeError' || (err.message?.includes('fetch') && err.name !== 'AbortError')) {
          console.warn('Streaming request failed, falling back to non-streaming:', err.message)
          try {
            const fallbackResponse = await this.generateContent(request)
            onComplete(fallbackResponse)
            resolve()
            return
          } catch (fallbackErr: any) {
            onError(fallbackErr.message || 'Content generation failed (streaming and fallback both failed)')
            reject(fallbackErr)
            return
          }
        }
        // For abort errors (browser/user cancelled), don't fallback - just report the error
        if (err.name === 'AbortError') {
          onError('Content generation was cancelled')
          reject(err)
          return
        }
        onError(err.message || 'Content generation failed')
        reject(err)
      }
    })
  }
}

// Create singleton instance
export const apiClient = new ApiClient()
export default apiClient
