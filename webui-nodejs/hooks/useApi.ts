import { useState, useEffect, useCallback } from 'react'
import useSWR, { useSWRConfig } from 'swr'
import apiClient from '../lib/api'
import { UseApiOptions } from '../types'

// Custom hook for API calls with SWR
export function useApi<T>(
  key: string | null,
  fetcher: () => Promise<T>,
  options: UseApiOptions = {}
) {
  const { data, error, mutate, isLoading, isValidating } = useSWR(
    key,
    fetcher,
    {
      revalidateOnFocus: false,
      revalidateOnReconnect: true,
      refreshInterval: options.refetchInterval,
      onSuccess: options.onSuccess,
      onError: options.onError,
    }
  )

  return {
    data,
    error,
    isLoading: isLoading || isValidating,
    mutate,
    refetch: mutate,
  }
}

// Dashboard hooks
export function useDashboardStats() {
  return useApi(
    'dashboard-stats',
    () => apiClient.getDashboardStats(),
    { refetchInterval: 30000 } // Refresh every 30 seconds
  )
}

// Theme hooks
export function useThemes() {
  return useApi('themes', () => apiClient.getThemes())
}

export function useTheme(id: number | null) {
  return useApi(
    id ? `theme-${id}` : null,
    () => apiClient.getTheme(id!)
  )
}

export function useTopicsByTheme(themeId: number | null, page = 0, size = 20) {
  return useApi(
    themeId ? `topics-theme-${themeId}-${page}-${size}` : null,
    () => apiClient.getTopicsByTheme(themeId!, page, size)
  )
}

// Topic hooks
export function useTopics(filters?: any, sort?: any, page = 0, size = 20) {
  const key = `topics-${JSON.stringify(filters)}-${JSON.stringify(sort)}-${page}-${size}`
  return useApi(key, () => apiClient.getTopics(filters, sort, page, size))
}

export function useTopic(id: number | null) {
  return useApi(
    id ? `topic-${id}` : null,
    () => apiClient.getTopic(id!)
  )
}

export function useRecentTopics(limit = 10) {
  return useApi(
    `recent-topics-${limit}`,
    () => apiClient.getRecentTopics(limit),
    { refetchInterval: 60000 } // Refresh every minute
  )
}

export function usePopularTopics(limit = 10) {
  return useApi(
    `popular-topics-${limit}`,
    () => apiClient.getPopularTopics(limit),
    { refetchInterval: 300000 } // Refresh every 5 minutes
  )
}

// Source hooks
export function useSources() {
  return useApi('sources', () => apiClient.getSources())
}

export function useSource(id: number | null) {
  return useApi(
    id ? `source-${id}` : null,
    () => apiClient.getSource(id!)
  )
}

// Search Topic hooks
export function useSearchTopics(sourceId?: number) {
  const key = sourceId ? `search-topics-${sourceId}` : 'search-topics'
  return useApi(key, () => apiClient.getSearchTopics(sourceId))
}

export function useSearchTopic(id: number | null) {
  return useApi(
    id ? `search-topic-${id}` : null,
    () => apiClient.getSearchTopic(id!)
  )
}

// Scan History hooks
export function useScanHistory(sourceId?: number, limit = 50) {
  const key = sourceId ? `scan-history-${sourceId}-${limit}` : `scan-history-${limit}`
  return useApi(key, () => apiClient.getScanHistory(sourceId, limit))
}

// Monitoring hooks
export function useHealthStatus() {
  return useApi(
    'health-status',
    () => apiClient.getHealthStatus(),
    { refetchInterval: 10000 } // Refresh every 10 seconds
  )
}

export function useMetrics() {
  return useApi(
    'metrics',
    () => apiClient.getMetrics(),
    { refetchInterval: 30000 } // Refresh every 30 seconds
  )
}

// Prompt Management hooks
export function usePromptTemplates() {
  return useApi('prompt-templates', () => apiClient.getPromptTemplates())
}

export function useDefaultPromptTemplates() {
  return useApi('default-prompt-templates', () => apiClient.getDefaultPromptTemplates())
}

// Mutation hooks
export function useCreateTheme() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (theme: any) => {
    const newTheme = await apiClient.createTheme(theme)
    mutate('themes')
    return newTheme
  }, [mutate])
}

export function useUpdateTheme() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number, theme: any) => {
    const updatedTheme = await apiClient.updateTheme(id, theme)
    mutate('themes')
    mutate(`theme-${id}`)
    return updatedTheme
  }, [mutate])
}

export function useDeleteTheme() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number) => {
    await apiClient.deleteTheme(id)
    mutate('themes')
    mutate(`theme-${id}`, undefined, false)
  }, [mutate])
}

export function useCreateSource() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (source: any) => {
    const newSource = await apiClient.createSource(source)
    mutate('sources')
    return newSource
  }, [mutate])
}

export function useUpdateSource() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number, source: any) => {
    const updatedSource = await apiClient.updateSource(id, source)
    mutate('sources')
    mutate(`source-${id}`)
    return updatedSource
  }, [mutate])
}

export function useDeleteSource() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number) => {
    await apiClient.deleteSource(id)
    mutate('sources')
    mutate(`source-${id}`, undefined, false)
  }, [mutate])
}

export function useScanSource() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number) => {
    await apiClient.scanSource(id)
    mutate('sources')
    mutate(`source-${id}`)
    mutate('scan-history')
  }, [mutate])
}

export function useCreateSearchTopic() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (searchTopic: any) => {
    const newSearchTopic = await apiClient.createSearchTopic(searchTopic)
    mutate('search-topics')
    if (searchTopic.sourceId) {
      mutate(`search-topics-${searchTopic.sourceId}`)
    }
    return newSearchTopic
  }, [mutate])
}

export function useUpdateSearchTopic() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number, searchTopic: any) => {
    const updatedSearchTopic = await apiClient.updateSearchTopic(id, searchTopic)
    mutate('search-topics')
    mutate(`search-topic-${id}`)
    if (searchTopic.sourceId) {
      mutate(`search-topics-${searchTopic.sourceId}`)
    }
    return updatedSearchTopic
  }, [mutate])
}

export function useDeleteSearchTopic() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number) => {
    await apiClient.deleteSearchTopic(id)
    mutate('search-topics')
    mutate(`search-topic-${id}`, undefined, false)
  }, [mutate])
}

export function useToggleSearchTopicStatus() {
  const { mutate } = useSWRConfig()
  
  return useCallback(async (id: number) => {
    const updatedSearchTopic = await apiClient.toggleSearchTopicStatus(id)
    mutate('search-topics')
    mutate(`search-topic-${id}`)
    return updatedSearchTopic
  }, [mutate])
}

// Search hook
export function useSearch() {
  return useCallback(async (query: string, filters?: any) => {
    return await apiClient.searchTopics(query, filters)
  }, [])
}

// Export hook
export function useExport() {
  return useCallback(async (format: 'csv' | 'json' | 'xlsx', filters?: any) => {
    const blob = await apiClient.exportTopics(format, filters)
    
    // Create download link
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `topics.${format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }, [])
}

// Test prompt template hook
export function useTestPromptTemplate() {
  return useCallback(async (templateName: string, variables: Record<string, any>) => {
    return await apiClient.testPromptTemplate(templateName, variables)
  }, [])
}
