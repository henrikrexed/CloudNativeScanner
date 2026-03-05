import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import { useRouter } from 'next/router'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { 
  ArrowLeftIcon,
  ArrowTopRightOnSquareIcon,
  TagIcon,
  CalendarIcon,
  UserIcon,
  FireIcon,
  ChatBubbleLeftRightIcon,
  EyeIcon,
  StarIcon,
  SparklesIcon
} from '@heroicons/react/24/outline'
import Layout from '../../../components/Layout'
import apiClient from '../../../lib/api'
import { Topic } from '../../../types'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

export default function ClusterDetailPage() {
  const router = useRouter()
  const { groupId } = router.query
  const [clusterData, setClusterData] = useState<{
    groupId: number
    representativeTopic: { id: number; title: string; url: string } | null
    topicCount: number
    summary: string | null
    clusterName: string | null
    tags: string[]
    topics: Topic[]
  } | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!groupId) return

    const fetchClusterData = async () => {
      try {
        setLoading(true)
        const groupIdNum = typeof groupId === 'string' ? parseInt(groupId, 10) : Array.isArray(groupId) ? parseInt(groupId[0], 10) : Number(groupId)
        
        const data = await apiClient.getClusterDetails(groupIdNum)
        setClusterData(data)
      } catch (err: any) {
        setError(err.message || 'Failed to load cluster')
        console.error('Error fetching cluster:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchClusterData()
  }, [groupId])

  // Group topics by source
  const topicsBySource = clusterData?.topics.reduce((acc, topic) => {
    const sourceName = topic.source?.name || 'Unknown'
    if (!acc[sourceName]) {
      acc[sourceName] = []
    }
    acc[sourceName].push(topic)
    return acc
  }, {} as Record<string, Topic[]>) || {}

  return (
    <Layout>
      <Head>
        <title>
          {clusterData?.clusterName 
            ? `${clusterData.clusterName} - Cloud Native Scanner` 
            : clusterData?.representativeTopic?.title 
              ? `Cluster: ${clusterData.representativeTopic.title} - Cloud Native Scanner` 
              : 'Cluster Details'}
        </title>
        <meta name="description" content={clusterData?.summary?.substring(0, 160) || 'Cluster of related topics'} />
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
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading cluster...</p>
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

        {/* Cluster Details */}
        {!loading && !error && clusterData && (
          <>
            {/* Cluster Header */}
            <motion.div variants={fadeInUp} className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-8">
              <div className="flex items-start justify-between gap-4 mb-6">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-4">
                    <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                      <SparklesIcon className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                    </div>
                    <div>
                      <h1 className="text-3xl font-bold text-gray-900 dark:text-white">
                        {clusterData.clusterName || (clusterData.representativeTopic ? clusterData.representativeTopic.title : `Topic Cluster ${clusterData.groupId}`)}
                      </h1>
                      <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
                        {clusterData.topicCount} related topic{clusterData.topicCount !== 1 ? 's' : ''}
                      </p>
                    </div>
                  </div>

                  {clusterData.representativeTopic && (
                    <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
                      <p className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                        Representative Topic:
                      </p>
                      <Link
                        href={`/topics/${clusterData.representativeTopic.id}`}
                        className="text-lg font-medium text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 transition-colors"
                      >
                        {clusterData.representativeTopic.title}
                      </Link>
                    </div>
                  )}
                </div>
              </div>

              {/* Cluster Summary */}
              {clusterData.summary && (
                <div className="mb-6 p-6 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl border border-blue-200 dark:border-blue-800">
                  <div className="flex items-center gap-2 mb-3">
                    <SparklesIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                    <h2 className="text-lg font-semibold text-blue-900 dark:text-blue-200">
                      Cluster Summary
                    </h2>
                  </div>
                  <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                    {clusterData.summary}
                  </p>
                </div>
              )}

              {!clusterData.summary && (
                <div className="mb-6 p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
                  <p className="text-sm text-yellow-800 dark:text-yellow-200">
                    Cluster summary is being generated. Please refresh the page in a moment.
                  </p>
                </div>
              )}

              {/* Cluster Tags */}
              {clusterData.tags && clusterData.tags.length > 0 && (
                <div className="mb-6">
                  <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                    Common Tags:
                  </h3>
                  <div className="flex flex-wrap gap-2">
                    {clusterData.tags.map((tag: string, index: number) => (
                      <span
                        key={index}
                        className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-xs font-medium"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </motion.div>

            {/* Topics in Cluster */}
            <motion.div variants={fadeInUp}>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
                Topics in This Cluster ({clusterData.topics.length})
              </h2>
              <p className="text-gray-600 dark:text-gray-400 mb-6">
                All topics grouped together because they discuss similar themes
              </p>

              <div className="space-y-6">
                {Object.entries(topicsBySource).map(([sourceName, topics]) => (
                  <motion.div
                    key={sourceName}
                    variants={fadeInUp}
                    className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-6"
                  >
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                      <TagIcon className="h-5 w-5 text-primary-600 dark:text-primary-400" />
                      {sourceName} ({topics.length})
                    </h3>
                    
                    <div className="space-y-4">
                      {topics.map((topic) => (
                        <Link
                          key={topic.id}
                          href={`/topics/${topic.id}`}
                          className="block p-4 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-primary-300 dark:hover:border-primary-700 transition-colors group"
                        >
                          <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                              <h4 className="text-base font-medium text-gray-900 dark:text-white mb-2 line-clamp-2 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                                {topic.title}
                              </h4>
                              
                              <div className="flex flex-wrap items-center gap-3 text-sm text-gray-500 dark:text-gray-400 mb-3">
                                {topic.interactionCount != null && topic.interactionCount > 0 && (
                                  <span className="inline-flex items-center gap-1">
                                    <ChatBubbleLeftRightIcon className="h-4 w-4" />
                                    {topic.interactionCount} replies
                                  </span>
                                )}
                                {topic.viewCount != null && topic.viewCount > 0 && (
                                  <span className="inline-flex items-center gap-1">
                                    <EyeIcon className="h-4 w-4" />
                                    {topic.viewCount.toLocaleString()} views
                                  </span>
                                )}
                                {topic.score != null && topic.score > 0 && (
                                  <span className="inline-flex items-center gap-1">
                                    <StarIcon className="h-4 w-4" />
                                    Score: {topic.score}
                                  </span>
                                )}
                                {topic.engagementScore != null && topic.engagementScore > 0 && (
                                  <span className="inline-flex items-center gap-1 text-orange-600 dark:text-orange-400">
                                    <FireIcon className="h-4 w-4" />
                                    Engagement: {topic.engagementScore.toFixed(1)}
                                  </span>
                                )}
                              </div>
                            </div>
                            
                            <div className="flex-shrink-0 flex items-center gap-2">
                              <a
                                href={topic.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                onClick={(e) => e.stopPropagation()}
                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300 hover:bg-primary-100 dark:hover:bg-primary-900/30 transition-colors text-sm font-medium"
                              >
                                <ArrowTopRightOnSquareIcon className="h-4 w-4" />
                                Original
                              </a>
                              <span className="text-primary-600 dark:text-primary-400 text-sm font-medium group-hover:text-primary-700 dark:group-hover:text-primary-300">
                                View Details →
                              </span>
                            </div>
                          </div>
                        </Link>
                      ))}
                    </div>
                  </motion.div>
                ))}
              </div>
            </motion.div>
          </>
        )}
      </motion.div>
    </Layout>
  )
}

