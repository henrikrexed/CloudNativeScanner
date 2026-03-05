import React, { useState } from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { useRouter } from 'next/router'
import { motion } from 'framer-motion'
import { 
  MagnifyingGlassIcon,
  DocumentTextIcon,
  TagIcon,
  CalendarIcon,
  ArrowRightIcon,
  FunnelIcon,
  SparklesIcon
} from '@heroicons/react/24/outline'
import Layout from '../components/Layout'
import apiClient from '../lib/api'
import { Topic, Source, Theme } from '../types'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

interface Cluster {
  groupId: number
  representativeTopicId: number
  representativeTitle: string
  clusterName?: string // Generated cluster name (preferred over representativeTitle)
  topicCount: number
  tags: string[]
  topics: Array<{
    id: number
    title: string
    url: string
    source: string | null
    engagementScore: number | null
    tags: string[]
  }>
}

export default function TopicsPage() {
  const router = useRouter()
  const { themeId, sourceId } = router.query
  const [clusters, setClusters] = useState<Cluster[]>([])
  const [sources, setSources] = useState<Source[]>([])
  const [themes, setThemes] = useState<Theme[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedSourceId, setSelectedSourceId] = useState<number | undefined>(
    sourceId ? (typeof sourceId === 'string' ? parseInt(sourceId, 10) : Array.isArray(sourceId) ? parseInt(sourceId[0], 10) : Number(sourceId)) : undefined
  )
  const [selectedThemeId, setSelectedThemeId] = useState<number | undefined>(
    themeId ? (typeof themeId === 'string' ? parseInt(themeId, 10) : Array.isArray(themeId) ? parseInt(themeId[0], 10) : Number(themeId)) : undefined
  )

  // Sync state with URL query parameters when they change
  React.useEffect(() => {
    const newSourceId = sourceId 
      ? (typeof sourceId === 'string' ? parseInt(sourceId, 10) : Array.isArray(sourceId) ? parseInt(sourceId[0], 10) : Number(sourceId))
      : undefined
    const newThemeId = themeId
      ? (typeof themeId === 'string' ? parseInt(themeId, 10) : Array.isArray(themeId) ? parseInt(themeId[0], 10) : Number(themeId))
      : undefined
    
    if (newSourceId !== selectedSourceId) {
      setSelectedSourceId(newSourceId)
    }
    if (newThemeId !== selectedThemeId) {
      setSelectedThemeId(newThemeId)
    }
  }, [router.query.sourceId, router.query.themeId])

  // Fetch sources and themes on mount
  React.useEffect(() => {
    const fetchSources = async () => {
      try {
        const sourcesData = await apiClient.getSources().catch(() => [])
        setSources(sourcesData || [])
      } catch (err) {
        console.error('Error fetching sources:', err)
      }
    }
    fetchSources()
    
    const fetchThemes = async () => {
      try {
        const themesData = await apiClient.getThemes().catch(() => [])
        setThemes(themesData || [])
      } catch (err) {
        console.error('Error fetching themes:', err)
      }
    }
    fetchThemes()
  }, [])

  // Fetch clusters when filters change
  React.useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        setError(null)
        
        console.log('🔍 Fetching clusters with filters:', { themeId: selectedThemeId, sourceId: selectedSourceId })
        
        // Fetch clusters - filter by theme if specified
        const clustersResponse = await apiClient.getClusters(0, 50, selectedThemeId).catch((err) => {
          console.error('❌ Error fetching clusters:', err)
          setError(`Failed to load clusters: ${err.message || 'Unknown error'}`)
          return { content: [], totalElements: 0 }
        })
        
        console.log('📦 Clusters response:', clustersResponse)
        
        let clustersList = clustersResponse?.content || []
        console.log(`📊 Found ${clustersList.length} clusters before source filtering`)
        
        // Filter by source if specified
        if (selectedSourceId) {
          const source = sources.find(s => s.id === selectedSourceId)
          console.log('🔍 Filtering by source:', source?.name)
          clustersList = clustersList.filter((cluster: Cluster) => 
            cluster.topics && cluster.topics.some(topic => {
              return source && topic.source === source.name
            })
          )
          console.log(`📊 After source filtering: ${clustersList.length} clusters`)
        }
        
        console.log(`✅ Setting ${clustersList.length} clusters (themeId: ${selectedThemeId}, sourceId: ${selectedSourceId})`)
        setClusters(clustersList)
        
        if (clustersList.length === 0 && !selectedThemeId && !selectedSourceId) {
          console.warn('⚠️ No clusters found. This might mean topics haven\'t been clustered yet.')
        }
      } catch (err: any) {
        const errorMsg = err.message || 'Failed to load clusters'
        setError(errorMsg)
        console.error('❌ Error fetching clusters:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [selectedSourceId, selectedThemeId, sources])

  const filteredClusters = clusters.filter(cluster => {
    const clusterName = cluster.clusterName || cluster.representativeTitle
    return clusterName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
           cluster.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
  })

  return (
    <Layout>
      <Head>
        <title>{selectedThemeId ? 'Topics by Theme' : 'Topics'} - Cloud Native Scanner</title>
        <meta name="description" content="Browse all cloud native topics" />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={{ animate: { transition: { staggerChildren: 0.1 } } }}
        className="space-y-6"
      >
        {/* Header */}
        <motion.div variants={fadeInUp} className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
              {selectedThemeId ? 'Clustered Topics by Theme' : 'Clustered Topics'}
            </h1>
            <p className="text-gray-600 dark:text-gray-400">
              {selectedThemeId 
                ? `Clustered topics filtered by theme (${clusters.length} cluster${clusters.length !== 1 ? 's' : ''} found)`
                : `Browse and explore clustered cloud native topics (${clusters.length} cluster${clusters.length !== 1 ? 's' : ''})`
              }
            </p>
          </div>
          {selectedThemeId && (
            <Link
              href="/topics"
              className="text-sm font-medium text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300"
            >
              Clear filter
            </Link>
          )}
          <Link href="/search">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-4 py-2.5 text-sm font-medium text-white shadow-lg hover:bg-primary-700 transition-colors"
            >
              <MagnifyingGlassIcon className="h-5 w-5" />
              New Search
            </motion.button>
          </Link>
        </motion.div>

        {/* Theme Filter Chips - Quick Access */}
        {themes.length > 0 && (
          <motion.div variants={fadeInUp} className="space-y-3">
            <div className="flex items-center gap-2">
              <TagIcon className="h-5 w-5 text-gray-500 dark:text-gray-400" />
              <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">Filter by Theme:</h3>
            </div>
            <div className="flex flex-wrap gap-2">
              {/* "All Themes" button */}
              <button
                onClick={() => {
                  setSelectedThemeId(undefined)
                  const query: any = { ...router.query }
                  delete query.themeId
                  if (selectedSourceId) {
                    query.sourceId = selectedSourceId.toString()
                  }
                  router.push({ pathname: router.pathname, query }, undefined, { shallow: true })
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                  !selectedThemeId
                    ? 'bg-primary-600 text-white shadow-md hover:bg-primary-700'
                    : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700'
                }`}
              >
                All Themes
              </button>
              {/* Theme chips */}
              {themes.map((theme) => (
                <button
                  key={theme.id}
                  onClick={() => {
                    setSelectedThemeId(theme.id)
                    const query: any = { ...router.query }
                    query.themeId = theme.id.toString()
                    if (selectedSourceId) {
                      query.sourceId = selectedSourceId.toString()
                    }
                    router.push({ pathname: router.pathname, query }, undefined, { shallow: true })
                  }}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${
                    selectedThemeId === theme.id
                      ? 'bg-primary-600 text-white shadow-md hover:bg-primary-700'
                      : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700'
                  }`}
                >
                  {theme.name}
                </button>
              ))}
            </div>
          </motion.div>
        )}

        {/* Filters and Search Bar */}
        <motion.div variants={fadeInUp} className="space-y-4">
          <div className="flex flex-col sm:flex-row gap-4">
            {/* Source Filter */}
            <div className="relative flex-shrink-0 sm:w-64">
              <FunnelIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400 dark:text-gray-500 z-10" />
              <select
                value={selectedSourceId || ''}
                onChange={(e) => {
                  const newSourceId = e.target.value ? parseInt(e.target.value) : undefined
                  setSelectedSourceId(newSourceId)
                  // Update URL query params to persist filter
                  const query: any = { ...router.query }
                  if (newSourceId) {
                    query.sourceId = newSourceId.toString()
                  } else {
                    delete query.sourceId
                  }
                  // Preserve theme filter when changing source
                  if (selectedThemeId) {
                    query.themeId = selectedThemeId.toString()
                  }
                  router.push({ pathname: router.pathname, query }, undefined, { shallow: true })
                }}
                className="w-full pl-12 pr-4 py-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent text-gray-900 dark:text-white appearance-none cursor-pointer transition-all"
              >
                <option value="">All Sources</option>
                {sources.map((source) => (
                  <option key={source.id} value={source.id}>
                    {source.name}
                  </option>
                ))}
              </select>
            </div>
            
            {/* Search Bar */}
            <div className="relative flex-1">
              <MagnifyingGlassIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400 dark:text-gray-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search clusters by title or tags..."
                className="w-full pl-12 pr-4 py-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-all"
              />
            </div>
          </div>
          
          {/* Active Filters Display */}
          {selectedSourceId && (
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm text-gray-600 dark:text-gray-400">Active filter:</span>
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-sm font-medium">
                <FunnelIcon className="h-3.5 w-3.5" />
                Source: {sources.find(s => s.id === selectedSourceId)?.name || 'Unknown Source'}
                <button
                  onClick={() => {
                    setSelectedSourceId(undefined)
                    const query: any = { ...router.query }
                    delete query.sourceId
                    if (selectedThemeId) {
                      query.themeId = selectedThemeId.toString()
                    }
                    router.push({ pathname: router.pathname, query }, undefined, { shallow: true })
                  }}
                  className="ml-1 hover:text-primary-900 dark:hover:text-primary-100"
                  aria-label="Clear source filter"
                >
                  ×
                </button>
              </span>
            </div>
          )}
        </motion.div>

        {/* Loading State */}
        {loading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary-200 border-t-primary-600"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading topics...</p>
          </motion.div>
        )}

        {/* Error State */}
        {error && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-4"
          >
            <p className="text-red-800 dark:text-red-200 font-semibold mb-2">Error loading clusters</p>
            <p className="text-red-700 dark:text-red-300 text-sm">{error}</p>
            <p className="text-red-600 dark:text-red-400 text-xs mt-2">
              Check browser console for more details. If clusters are empty, topics may not have been clustered yet.
            </p>
          </motion.div>
        )}

        {/* Empty State */}
        {!loading && !error && filteredClusters.length === 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <DocumentTextIcon className="h-12 w-12 text-gray-400 dark:text-gray-500 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              {searchQuery 
                ? 'No clusters found' 
                : selectedThemeId 
                  ? 'No clusters found for this theme' 
                  : 'No clusters yet'}
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6">
              {selectedThemeId 
                ? 'Clustered topics may not be available for this theme yet. Topics need to be clustered first. Try browsing all clusters or selecting a different theme.'
                : searchQuery
                  ? 'Try adjusting your search query'
                  : 'No clusters found. Topics need to be clustered by the topic grouping service. This happens automatically when topics are processed.'}
            </p>
            {!searchQuery && (
              <Link href="/search">
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary-600 px-6 py-3 text-sm font-medium text-white shadow-lg hover:bg-primary-700 transition-colors"
                >
                  <MagnifyingGlassIcon className="h-5 w-5" />
                  Start Searching
                </motion.button>
              </Link>
            )}
          </motion.div>
        )}

        {/* Clusters List */}
        {!loading && !error && filteredClusters.length > 0 && (
          <div className="grid grid-cols-1 gap-4">
            {filteredClusters.map((cluster, index) => (
              <motion.div
                key={cluster.groupId}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
                className="group rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 hover:shadow-soft-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300 overflow-hidden"
              >
                <Link 
                  href={`/topics/cluster/${cluster.groupId}`}
                  className="block p-6"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2">
                        <SparklesIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-2">
                          {cluster.clusterName || cluster.representativeTitle}
                        </h3>
                      </div>
                      <div className="flex flex-wrap items-center gap-3 text-sm text-gray-500 dark:text-gray-400 mb-2">
                        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 font-medium">
                          {cluster.topicCount} {cluster.topicCount === 1 ? 'topic' : 'topics'}
                        </span>
                        {cluster.tags && cluster.tags.length > 0 && (
                          <span className="inline-flex items-center gap-1.5">
                            <TagIcon className="h-4 w-4" />
                            {cluster.tags.slice(0, 3).join(', ')}
                            {cluster.tags.length > 3 && ` +${cluster.tags.length - 3} more`}
                          </span>
                        )}
                      </div>
                      {cluster.topics && cluster.topics.length > 0 && (
                        <div className="text-xs text-gray-400 dark:text-gray-500 mt-2">
                          Sources: {Array.from(new Set(cluster.topics.map(t => t.source).filter(Boolean))).join(', ')}
                        </div>
                      )}
                    </div>
                    <ArrowRightIcon className="h-5 w-5 text-gray-400 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors flex-shrink-0 mt-1" />
                  </div>
                </Link>
              </motion.div>
            ))}
          </div>
        )}

        {/* Results Count */}
        {!loading && !error && filteredClusters.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="text-center text-sm text-gray-500 dark:text-gray-400"
          >
            Showing {filteredClusters.length} of {clusters.length} clusters
            {selectedThemeId && ` in theme "${themes.find(t => t.id === selectedThemeId)?.name || 'selected theme'}"`}
            {selectedSourceId && ` from ${sources.find(s => s.id === selectedSourceId)?.name || 'selected source'}`}
            {searchQuery && ` matching "${searchQuery}"`}
          </motion.div>
        )}
      </motion.div>
    </Layout>
  )
}
