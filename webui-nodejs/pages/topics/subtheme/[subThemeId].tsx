import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { 
  ArrowLeftIcon,
  ArrowTopRightOnSquareIcon,
  TagIcon,
  FireIcon,
  ChatBubbleLeftRightIcon,
  EyeIcon,
  StarIcon,
  MagnifyingGlassIcon
} from '@heroicons/react/24/outline'
import Layout from '../../../components/Layout'
import apiClient from '../../../lib/api'
import { Topic, Theme } from '../../../types'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

export default function SubThemeTopicsPage() {
  const router = useRouter()
  const { subThemeId } = router.query
  const [subTheme, setSubTheme] = useState<Theme | null>(null)
  const [topics, setTopics] = useState<Topic[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [sortBy, setSortBy] = useState<'hottest' | 'recent'>('hottest')

  useEffect(() => {
    if (!subThemeId) return

    const fetchData = async () => {
      try {
        setLoading(true)
        const themeId = typeof subThemeId === 'string' ? parseInt(subThemeId, 10) : Array.isArray(subThemeId) ? parseInt(subThemeId[0], 10) : Number(subThemeId)
        
        // Fetch sub-theme details and hottest topics
        const [themeData, hottestTopics] = await Promise.all([
          apiClient.getTheme(themeId).catch(() => null),
          apiClient.getHottestTopicsBySubTheme(themeId, 50).catch(() => [])
        ])
        
        if (!themeData) {
          setError('Sub-theme not found')
          return
        }
        
        setSubTheme(themeData)
        setTopics(hottestTopics || [])
      } catch (err: any) {
        setError(err.message || 'Failed to load topics')
        console.error('Error fetching sub-theme topics:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [subThemeId])

  // Filter and sort topics
  const filteredTopics = topics
    .filter(topic =>
      topic.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      topic.content?.toLowerCase().includes(searchQuery.toLowerCase())
    )
    .sort((a, b) => {
      if (sortBy === 'hottest') {
        // Sort by interactions first, then engagement score
        const aInteractions = a.interactionCount || 0
        const bInteractions = b.interactionCount || 0
        if (bInteractions !== aInteractions) {
          return bInteractions - aInteractions
        }
        const aEngagement = a.engagementScore || 0
        const bEngagement = b.engagementScore || 0
        return bEngagement - aEngagement
      } else {
        // Sort by recent (created date)
        const aDate = new Date(a.createdAt || 0).getTime()
        const bDate = new Date(b.createdAt || 0).getTime()
        return bDate - aDate
      }
    })

  return (
    <Layout>
      <Head>
        <title>
          {subTheme ? `${subTheme.fullPath || subTheme.name} - Topics` : 'Sub-Theme Topics'} - Cloud Native Scanner
        </title>
        <meta name="description" content={`Browse hottest conversations in ${subTheme?.name || 'sub-theme'}`} />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={{ animate: { transition: { staggerChildren: 0.1 } } }}
        className="space-y-6"
      >
        {/* Back Button */}
        <motion.div variants={fadeInUp}>
          <Link href="/topics">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="inline-flex items-center gap-2 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white transition-colors"
            >
              <ArrowLeftIcon className="h-5 w-5" />
              Back to Topics
            </motion.button>
          </Link>
        </motion.div>

        {/* Loading State */}
        {loading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary-200 border-t-primary-600"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading conversations...</p>
          </motion.div>
        )}

        {/* Error State */}
        {error && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="rounded-xl bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 p-4"
          >
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </motion.div>
        )}

        {/* Sub-Theme Header */}
        {!loading && !error && subTheme && (
          <motion.div variants={fadeInUp} className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-lg bg-primary-100 dark:bg-primary-900/30">
                <TagIcon className="h-6 w-6 text-primary-600 dark:text-primary-400" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                  {subTheme.fullPath || subTheme.name}
                </h1>
                {subTheme.description && (
                  <p className="text-gray-600 dark:text-gray-400 mt-1">{subTheme.description}</p>
                )}
                {subTheme.parentTheme && (
                  <p className="text-sm text-gray-500 dark:text-gray-500 mt-1">
                    Parent: <Link href={`/topics/subtheme/${subTheme.parentTheme.id}`} className="text-primary-600 dark:text-primary-400 hover:underline">
                      {subTheme.parentTheme.name}
                    </Link>
                  </p>
                )}
              </div>
            </div>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Showing {filteredTopics.length} hottest conversations sorted by interactions
            </p>
          </motion.div>
        )}

        {/* Search and Sort Controls */}
        {!loading && !error && subTheme && (
          <motion.div variants={fadeInUp} className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <MagnifyingGlassIcon className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400 dark:text-gray-500" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search conversations..."
                className="w-full pl-12 pr-4 py-3 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-xl focus:ring-2 focus:ring-primary-500 focus:border-transparent text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 transition-all"
              />
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setSortBy('hottest')}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  sortBy === 'hottest'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                }`}
              >
                <FireIcon className="h-5 w-5 inline mr-2" />
                Hottest
              </button>
              <button
                onClick={() => setSortBy('recent')}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  sortBy === 'recent'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200 dark:hover:bg-gray-600'
                }`}
              >
                Recent
              </button>
            </div>
          </motion.div>
        )}

        {/* Topics List - Hottest Conversations */}
        {!loading && !error && subTheme && filteredTopics.length > 0 && (
          <motion.div variants={fadeInUp} className="space-y-4">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
              Hottest Conversations ({filteredTopics.length})
            </h2>
            <div className="space-y-4">
              {filteredTopics.map((topic, index) => (
                <motion.div
                  key={topic.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.05 }}
                  className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-6 hover:shadow-soft-lg hover:border-primary-200 dark:hover:border-primary-800 transition-all"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-3 line-clamp-2">
                        {topic.title}
                      </h3>
                      
                      {/* Engagement Metrics */}
                      <div className="flex flex-wrap items-center gap-4 mb-4 text-sm">
                        {topic.interactionCount != null && topic.interactionCount > 0 && (
                          <div className="flex items-center gap-1.5 text-blue-600 dark:text-blue-400">
                            <ChatBubbleLeftRightIcon className="h-4 w-4" />
                            <span className="font-medium">{topic.interactionCount}</span>
                            <span className="text-gray-600 dark:text-gray-400">
                              {topic.interactionCount === 1 ? 'reply' : 'replies'}
                            </span>
                          </div>
                        )}
                        {topic.viewCount != null && topic.viewCount > 0 && (
                          <div className="flex items-center gap-1.5 text-green-600 dark:text-green-400">
                            <EyeIcon className="h-4 w-4" />
                            <span className="font-medium">{topic.viewCount.toLocaleString()}</span>
                            <span className="text-gray-600 dark:text-gray-400">views</span>
                          </div>
                        )}
                        {topic.score != null && topic.score > 0 && (
                          <div className="flex items-center gap-1.5 text-yellow-600 dark:text-yellow-400">
                            <StarIcon className="h-4 w-4" />
                            <span className="font-medium">Score: {topic.score}</span>
                          </div>
                        )}
                        {topic.engagementScore != null && topic.engagementScore > 0 && (
                          <div className="flex items-center gap-1.5 text-orange-600 dark:text-orange-400">
                            <FireIcon className="h-4 w-4" />
                            <span className="font-medium">Engagement: {topic.engagementScore.toFixed(1)}</span>
                          </div>
                        )}
                        {topic.source && (
                          <span className="text-gray-600 dark:text-gray-400">
                            from {topic.source.name}
                          </span>
                        )}
                      </div>
                    </div>
                    
                    {/* Link to Original Source */}
                    <a
                      href={topic.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="flex-shrink-0 inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary-600 text-white hover:bg-primary-700 transition-colors font-medium text-sm"
                    >
                      <ArrowTopRightOnSquareIcon className="h-4 w-4" />
                      View Conversation
                    </a>
                  </div>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

        {/* Empty State */}
        {!loading && !error && subTheme && filteredTopics.length === 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <p className="text-gray-600 dark:text-gray-400">
              {searchQuery 
                ? `No conversations found matching "${searchQuery}"`
                : 'No conversations found in this sub-category yet.'}
            </p>
          </motion.div>
        )}
      </motion.div>
    </Layout>
  )
}
