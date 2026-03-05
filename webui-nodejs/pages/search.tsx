import React, { useState } from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { 
  MagnifyingGlassIcon,
  SparklesIcon,
  FunnelIcon,
  LightBulbIcon,
  DocumentTextIcon,
  ArrowTopRightOnSquareIcon,
  FireIcon
} from '@heroicons/react/24/outline'
import Layout from '../components/Layout'
import apiClient from '../lib/api'
import { Topic } from '../types'
import { formatDate } from '../lib/dateUtils'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

export default function SearchPage() {
  const [query, setQuery] = useState('')
  const [searching, setSearching] = useState(false)
  const [results, setResults] = useState<Topic[]>([])
  const [topicsWithScores, setTopicsWithScores] = useState<Array<{ topic: Topic; relevanceScore: number }>>([])
  const [error, setError] = useState<string | null>(null)
  const [hasSearched, setHasSearched] = useState(false)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const size = 20

  const handleSearch = async (e?: React.FormEvent, searchQuery?: string, pageNum: number = 0) => {
    if (e) {
      e.preventDefault()
    }
    
    const searchTerm = searchQuery || query.trim()
    if (!searchTerm) return

    setSearching(true)
    setError(null)
    setHasSearched(true)
    setPage(pageNum)

    try {
      const response = await apiClient.searchTopics(searchTerm, pageNum, size, 0.5)
      setResults(response.content || [])
      setTopicsWithScores(response.topicsWithScores || [])
      setTotalPages(response.totalPages || 0)
      setTotalElements(response.totalElements || 0)
    } catch (err: any) {
      console.error('Search error:', err)
      setError(err.response?.data?.error || 'Failed to search topics. Please try again.')
      setResults([])
      setTopicsWithScores([])
    } finally {
      setSearching(false)
    }
  }

  const handlePopularSearch = (term: string) => {
    setQuery(term)
    handleSearch(undefined, term, 0)
  }

  // Use the shared date utility instead of local function

  const getRelevanceScore = (topicId: number) => {
    const topicWithScore = topicsWithScores.find(t => t.topic.id === topicId)
    return topicWithScore?.relevanceScore || null
  }

  return (
    <Layout>
      <Head>
        <title>Search - Cloud Native Scanner</title>
        <meta name="description" content="Search for cloud native topics" />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={{ animate: { transition: { staggerChildren: 0.1 } } }}
        className="max-w-4xl mx-auto space-y-8"
      >
        {/* Header */}
        <motion.div variants={fadeInUp} className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 dark:text-white mb-3">
            Search Topics
          </h1>
          <p className="text-lg text-gray-600 dark:text-gray-400">
            Find cloud native topics across all sources
          </p>
        </motion.div>

        {/* Search Form */}
        <motion.form
          variants={fadeInUp}
          onSubmit={handleSearch}
          className="relative"
        >
          <div className="relative">
            <MagnifyingGlassIcon className="absolute left-6 top-1/2 transform -translate-y-1/2 h-6 w-6 text-gray-400 dark:text-gray-500" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handleSearch(e as any)
                }
              }}
              placeholder="Search for topics, keywords, technologies, or themes..."
              className="w-full pl-14 pr-6 py-4 bg-white dark:bg-gray-800 border-2 border-gray-200 dark:border-gray-700 rounded-2xl focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 text-lg transition-all shadow-soft hover:shadow-soft-lg"
            />
            <motion.button
              type="submit"
              disabled={searching || !query.trim()}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="absolute right-2 top-1/2 transform -translate-y-1/2 inline-flex items-center gap-2 rounded-xl bg-primary-600 px-6 py-3 text-sm font-medium text-white shadow-lg hover:bg-primary-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {searching ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                  Searching...
                </>
              ) : (
                <>
                  <MagnifyingGlassIcon className="h-5 w-5" />
                  Search
                </>
              )}
            </motion.button>
          </div>
        </motion.form>

        {/* Error Message */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-4"
          >
            <p className="text-sm text-red-800 dark:text-red-200">{error}</p>
          </motion.div>
        )}

        {/* Search Results */}
        {hasSearched && !searching && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-4"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                {totalElements > 0 ? (
                  <>Found {totalElements} {totalElements === 1 ? 'topic' : 'topics'}</>
                ) : (
                  'No topics found'
                )}
              </h2>
              {totalElements > 0 && (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  Results ordered by relevance and engagement
                </p>
              )}
            </div>

            {results.length > 0 ? (
              <>
                <div className="space-y-3">
                  {results.map((topic, index) => {
                    const relevanceScore = getRelevanceScore(topic.id)
                    return (
                      <motion.div
                        key={topic.id}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: index * 0.05 }}
                        className="group rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 hover:shadow-soft-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all duration-300 overflow-hidden"
                      >
                        <Link href={`/topics/${topic.id}`}>
                          <div className="p-6 cursor-pointer">
                            <div className="flex items-start justify-between gap-4">
                              <div className="flex-1 min-w-0">
                                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors line-clamp-2">
                                  {topic.title}
                                </h3>
                                {topic.content && (
                                  <p className="text-sm text-gray-600 dark:text-gray-400 mb-3 line-clamp-2">
                                    {topic.content.substring(0, 200)}...
                                  </p>
                                )}
                                <div className="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
                                  {topic.source?.name && (
                                    <span className="flex items-center gap-1">
                                      <DocumentTextIcon className="h-4 w-4" />
                                      {topic.source.name}
                                    </span>
                                  )}
                                  {topic.publishedAt && (
                                    <span>{formatDate(topic.publishedAt)}</span>
                                  )}
                                  {topic.interactionCount !== null && topic.interactionCount > 0 && (
                                    <span className="flex items-center gap-1">
                                      <FireIcon className="h-4 w-4" />
                                      {topic.interactionCount} interactions
                                    </span>
                                  )}
                                </div>
                              </div>
                              <div className="flex flex-col items-end gap-2">
                                {relevanceScore !== null && (
                                  <div className="px-3 py-1 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-xs font-medium">
                                    {Math.round(relevanceScore * 100)}% match
                                  </div>
                                )}
                                {topic.url && (
                                  <a
                                    href={topic.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    onClick={(e) => e.stopPropagation()}
                                    className="p-2 rounded-lg bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors"
                                  >
                                    <ArrowTopRightOnSquareIcon className="h-4 w-4 text-gray-600 dark:text-gray-400" />
                                  </a>
                                )}
                              </div>
                            </div>
                          </div>
                        </Link>
                      </motion.div>
                    )
                  })}
                </div>

                {/* Pagination */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 pt-4">
                    <button
                      onClick={() => handleSearch(undefined, query, page - 1)}
                      disabled={page === 0}
                      className="px-4 py-2 rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      Previous
                    </button>
                    <span className="px-4 py-2 text-sm text-gray-600 dark:text-gray-400">
                      Page {page + 1} of {totalPages}
                    </span>
                    <button
                      onClick={() => handleSearch(undefined, query, page + 1)}
                      disabled={page >= totalPages - 1}
                      className="px-4 py-2 rounded-lg bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                      Next
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-gray-500 dark:text-gray-400">
                  No topics found for "{query}". Try different keywords or check your spelling.
                </p>
              </div>
            )}
          </motion.div>
        )}

        {/* Search Tips */}
        <motion.div
          variants={fadeInUp}
          className="grid grid-cols-1 md:grid-cols-2 gap-6"
        >
          <div className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-6">
            <div className="flex items-start gap-4">
              <div className="p-2 rounded-lg bg-primary-100 dark:bg-primary-900/20">
                <LightBulbIcon className="h-6 w-6 text-primary-600 dark:text-primary-400" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-2">Search Tips</h3>
                <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
                  <li>• Use keywords to find specific topics</li>
                  <li>• Search by technology name</li>
                  <li>• Filter by theme or source</li>
                </ul>
              </div>
            </div>
          </div>

          <div className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-6">
            <div className="flex items-start gap-4">
              <div className="p-2 rounded-lg bg-accent-100 dark:bg-accent-900/20">
                <FunnelIcon className="h-6 w-6 text-accent-600 dark:text-accent-400" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900 dark:text-white mb-2">Advanced Filters</h3>
                <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
                  <li>• Filter by date range</li>
                  <li>• Sort by relevance</li>
                  <li>• Group by source</li>
                </ul>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Popular Searches */}
        <motion.div
          variants={fadeInUp}
          className="rounded-xl bg-gradient-to-br from-primary-50 to-accent-50 dark:from-primary-900/20 dark:to-accent-900/20 border border-primary-100 dark:border-primary-800 p-6"
        >
          <div className="flex items-center gap-2 mb-4">
            <SparklesIcon className="h-5 w-5 text-primary-600 dark:text-primary-400" />
            <h3 className="font-semibold text-gray-900 dark:text-white">Popular Searches</h3>
          </div>
          <div className="flex flex-wrap gap-2">
            {['Kubernetes', 'Docker', 'Helm', 'Prometheus', 'Grafana', 'Istio'].map((term) => (
              <button
                key={term}
                onClick={() => handlePopularSearch(term)}
                className="px-4 py-2 rounded-lg bg-white dark:bg-gray-800 text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-primary-50 dark:hover:bg-primary-900/20 hover:text-primary-600 dark:hover:text-primary-400 transition-colors border border-gray-200 dark:border-gray-700"
              >
                {term}
              </button>
            ))}
          </div>
        </motion.div>
      </motion.div>
    </Layout>
  )
}
