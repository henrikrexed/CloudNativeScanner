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
  HandThumbUpIcon,
  HandThumbDownIcon,
  SparklesIcon
} from '@heroicons/react/24/outline'
import { 
  HandThumbUpIcon as HandThumbUpIconSolid,
  HandThumbDownIcon as HandThumbDownIconSolid
} from '@heroicons/react/24/solid'
import Layout from '../../components/Layout'
import apiClient from '../../lib/api'
import { Topic, Source } from '../../types'
import { formatDateLong, formatDateTime } from '../../lib/dateUtils'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

export default function TopicDetailPage() {
  const router = useRouter()
  const { id, sourceId, themeId } = router.query
  const [topic, setTopic] = useState<Topic | null>(null)
  const [relatedTopics, setRelatedTopics] = useState<Topic[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [feedbackLoading, setFeedbackLoading] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null)
  const [tags, setTags] = useState<string[]>([])
  const [regeneratingTags, setRegeneratingTags] = useState(false)
  const [clusterInfo, setClusterInfo] = useState<{ groupId: number; topicCount: number } | null>(null)

  useEffect(() => {
    if (!id) return

    const fetchTopicData = async () => {
      try {
        setLoading(true)
        const topicId = typeof id === 'string' ? parseInt(id, 10) : Array.isArray(id) ? parseInt(id[0], 10) : Number(id)
        
        const [topicData, relatedData] = await Promise.all([
          apiClient.getTopic(topicId).catch(() => null),
          apiClient.getRelatedTopics(topicId).catch(() => [])
        ])
        
        if (!topicData) {
          setError('Topic not found')
          return
        }
        
        setTopic(topicData)
        setRelatedTopics(relatedData || [])
        
        // Check if topic is part of a cluster
        if (topicData.topicGroupId) {
          try {
            const clusterDetails = await apiClient.getClusterDetails(topicData.topicGroupId)
            setClusterInfo({
              groupId: clusterDetails.groupId,
              topicCount: clusterDetails.topicCount
            })
          } catch (err) {
            console.error('Error fetching cluster details:', err)
          }
        }
        
        // Parse tags from topic
        if (topicData.tagsList && Array.isArray(topicData.tagsList)) {
          setTags(topicData.tagsList)
        } else if (topicData.tags) {
          try {
            const parsed = JSON.parse(topicData.tags)
            setTags(Array.isArray(parsed) ? parsed : [])
          } catch {
            setTags(topicData.tags.split(',').map(t => t.trim()).filter(t => t))
          }
        } else {
          setTags([])
        }
      } catch (err: any) {
        setError(err.message || 'Failed to load topic')
        console.error('Error fetching topic:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchTopicData()
  }, [id])

  const handleThumbsUp = async () => {
    if (!topic || feedbackLoading) return
    
    try {
      setFeedbackLoading(true)
      setFeedbackMessage(null)
      const result = await apiClient.thumbsUpTopic(topic.id)
      setFeedbackMessage(result.message)
      
      // Update topic state
      setTopic({
        ...topic,
        thumbsUp: (topic.thumbsUp || 0) + 1,
        isRejected: false
      })
      
      setTimeout(() => setFeedbackMessage(null), 3000)
    } catch (err: any) {
      setFeedbackMessage(err.response?.data?.error || 'Failed to record feedback')
      setTimeout(() => setFeedbackMessage(null), 5000)
    } finally {
      setFeedbackLoading(false)
    }
  }

  const handleThumbsDown = async () => {
    if (!topic || feedbackLoading) return
    
    const reason = window.prompt('Why is this topic not relevant? (optional)', 'Not relevant or low quality')
    if (reason === null) return // User cancelled
    
    try {
      setFeedbackLoading(true)
      setFeedbackMessage(null)
      const result = await apiClient.thumbsDownTopic(topic.id, reason || undefined)
      setFeedbackMessage(result.message)
      
      // Update topic state and redirect after a delay
      setTopic({
        ...topic,
        thumbsDown: (topic.thumbsDown || 0) + 1,
        isRejected: true,
        rejectionReason: reason || undefined
      })
      
      // Redirect to topics list after showing message
      setTimeout(() => {
        router.push('/topics')
      }, 2000)
    } catch (err: any) {
      setFeedbackMessage(err.response?.data?.error || 'Failed to record feedback')
      setTimeout(() => setFeedbackMessage(null), 5000)
    } finally {
      setFeedbackLoading(false)
    }
  }

  const handleRegenerateTags = async () => {
    if (!topic || regeneratingTags) return
    
    try {
      setRegeneratingTags(true)
      setFeedbackMessage(null)
      const result = await apiClient.regenerateTopicTags(topic.id)
      setTags(result.tags)
      setFeedbackMessage(result.message)
      
      // Update topic state
      setTopic({
        ...topic,
        tagsList: result.tags
      })
      
      setTimeout(() => setFeedbackMessage(null), 3000)
    } catch (err: any) {
      setFeedbackMessage(err.response?.data?.error || 'Failed to regenerate tags')
      setTimeout(() => setFeedbackMessage(null), 5000)
    } finally {
      setRegeneratingTags(false)
    }
  }

  // Group related topics by source
  const topicsBySource = relatedTopics.reduce((acc, topic) => {
    const sourceName = topic.source?.name || 'Unknown'
    if (!acc[sourceName]) {
      acc[sourceName] = []
    }
    acc[sourceName].push(topic)
    return acc
  }, {} as Record<string, Topic[]>)

  return (
    <Layout>
      <Head>
        <title>{topic ? `${topic.title} - Cloud Native Scanner` : 'Topic Details'}</title>
        <meta name="description" content={topic?.content?.substring(0, 160) || 'Topic details'} />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={{ animate: { transition: { staggerChildren: 0.1 } } }}
        className="space-y-6"
      >
        {/* Back Button */}
        <motion.div variants={fadeInUp}>
          <Link 
            href={{
              pathname: '/topics',
              query: {
                ...(sourceId ? { sourceId: sourceId.toString() } : {}),
                ...(themeId ? { themeId: themeId.toString() } : {})
              }
            }}
          >
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
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading topic...</p>
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

        {/* Topic Details */}
        {!loading && !error && topic && (
          <>
            <motion.div variants={fadeInUp} className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-8">
              <div className="flex items-start justify-between gap-4 mb-6">
                <div className="flex-1">
                  <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
                    {topic.title}
                  </h1>
                  
                  <div className="flex flex-wrap items-center gap-4 text-sm text-gray-600 dark:text-gray-400 mb-4">
                    {topic.source && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 dark:bg-gray-700 rounded-lg">
                        <TagIcon className="h-4 w-4" />
                        <span className="font-medium">{topic.source.name}</span>
                      </span>
                    )}
                    {topic.author && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 dark:bg-gray-700 rounded-lg">
                        <UserIcon className="h-4 w-4" />
                        <span className="font-medium">{topic.author}</span>
                      </span>
                    )}
                    {topic.createdAt && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 dark:bg-gray-700 rounded-lg">
                        <CalendarIcon className="h-4 w-4" />
                        <span className="font-medium">
                          {formatDateLong(topic.createdAt)}
                        </span>
                      </span>
                    )}
                    {topic.metadata?.publishedAt && (
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-gray-100 dark:bg-gray-700 rounded-lg">
                        <CalendarIcon className="h-4 w-4" />
                        <span className="font-medium">
                          Published: {formatDateLong(topic.metadata.publishedAt)}
                        </span>
                      </span>
                    )}
                  </div>

                  {/* Engagement Metrics */}
                  <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-900/50 rounded-lg">
                    <div className="flex flex-wrap items-center gap-6 mb-3">
                      {topic.engagementScore != null && topic.engagementScore > 0 && (
                        <div className="flex items-center gap-2">
                          <FireIcon className="h-5 w-5 text-orange-500" />
                          <span className="font-semibold text-gray-900 dark:text-white">
                            Engagement: {topic.engagementScore.toFixed(1)}
                          </span>
                        </div>
                      )}
                      {topic.interactionCount != null && topic.interactionCount > 0 && (
                        <div className="flex items-center gap-2">
                          <ChatBubbleLeftRightIcon className="h-5 w-5 text-blue-500" />
                          <span className="text-gray-700 dark:text-gray-300">
                            {topic.interactionCount} {topic.interactionCount === 1 ? 'reply' : 'replies'}
                          </span>
                        </div>
                      )}
                      {topic.viewCount != null && topic.viewCount > 0 && (
                        <div className="flex items-center gap-2">
                          <EyeIcon className="h-5 w-5 text-green-500" />
                          <span className="text-gray-700 dark:text-gray-300">
                            {topic.viewCount.toLocaleString()} {topic.viewCount === 1 ? 'view' : 'views'}
                          </span>
                        </div>
                      )}
                      {topic.score != null && topic.score > 0 && (
                        <div className="flex items-center gap-2">
                          <StarIcon className="h-5 w-5 text-yellow-500" />
                          <span className="text-gray-700 dark:text-gray-300">
                            Score: {topic.score}
                          </span>
                        </div>
                      )}
                      {topic.isHotTopic && (
                        <div className="flex items-center gap-2 px-3 py-1 bg-red-100 dark:bg-red-900/30 rounded-lg">
                          <FireIcon className="h-5 w-5 text-red-600 dark:text-red-400" />
                          <span className="text-sm font-semibold text-red-700 dark:text-red-300">
                            Hot Topic
                          </span>
                        </div>
                      )}
                    </div>
                    
                    {/* Quality Scores */}
                    {(topic.technicalQualityScore != null || topic.marketingScore != null) && (
                      <div className="pt-3 border-t border-gray-200 dark:border-gray-700 flex flex-wrap items-center gap-4 text-xs">
                        {topic.technicalQualityScore != null && topic.technicalQualityScore > 0 && (
                          <div className="flex items-center gap-2">
                            <span className="text-gray-600 dark:text-gray-400">Technical Quality:</span>
                            <span className="font-semibold text-blue-600 dark:text-blue-400">
                              {(topic.technicalQualityScore * 100).toFixed(0)}%
                            </span>
                          </div>
                        )}
                        {topic.marketingScore != null && topic.marketingScore > 0 && (
                          <div className="flex items-center gap-2">
                            <span className="text-gray-600 dark:text-gray-400">Marketing Content:</span>
                            <span className="font-semibold text-purple-600 dark:text-purple-400">
                              {(topic.marketingScore * 100).toFixed(0)}%
                            </span>
                          </div>
                        )}
                      </div>
                    )}
                    
                    {/* Content Extraction Status */}
                    {topic.contentExtractionStatus && (
                      <div className="pt-3 border-t border-gray-200 dark:border-gray-700">
                        <div className="flex items-center gap-2 text-xs">
                          <span className="text-gray-600 dark:text-gray-400">Content Status:</span>
                          <span className={`font-semibold px-2 py-1 rounded ${
                            topic.contentExtractionStatus === 'COMPLETED' 
                              ? 'bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300'
                              : topic.contentExtractionStatus === 'PROCESSING'
                              ? 'bg-yellow-100 dark:bg-yellow-900/30 text-yellow-700 dark:text-yellow-300'
                              : topic.contentExtractionStatus === 'FAILED'
                              ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300'
                              : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300'
                          }`}>
                            {topic.contentExtractionStatus}
                          </span>
                          {topic.contentExtractionCompletedAt && (
                            <span className="text-gray-500 dark:text-gray-400">
                              (Completed: {formatDateTime(topic.contentExtractionCompletedAt)})
                            </span>
                          )}
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Summary (if available) */}
                  {topic.contentSummary ? (
                    <div className="mb-6 p-6 bg-gradient-to-r from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl border border-blue-200 dark:border-blue-800">
                      <div className="flex items-center gap-2 mb-3">
                        <SparklesIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                        <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-200">
                          AI-Generated Summary
                        </h3>
                      </div>
                      <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                        {topic.contentSummary}
                      </p>
                    </div>
                  ) : (
                    <div className="mb-6 p-4 bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 rounded-lg">
                      <p className="text-sm text-yellow-800 dark:text-yellow-200">
                        <SparklesIcon className="h-4 w-4 inline mr-2" />
                        Summary is being generated. This topic will be analyzed and summarized soon.
                      </p>
                    </div>
                  )}

                  {/* Conversation Summary (if available, for discussion threads) */}
                  {topic.conversationSummary ? (
                    <div className="mb-6 p-6 bg-gradient-to-r from-purple-50 to-pink-50 dark:from-purple-900/20 dark:to-pink-900/20 rounded-xl border border-purple-200 dark:border-purple-800">
                      <div className="flex items-center gap-2 mb-3">
                        <ChatBubbleLeftRightIcon className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                        <h3 className="text-lg font-semibold text-purple-900 dark:text-purple-200">
                          Conversation Summary
                        </h3>
                      </div>
                      <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                        {topic.conversationSummary}
                      </p>
                    </div>
                  ) : topic.interactionCount && topic.interactionCount > 0 && (
                    <div className="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                      <p className="text-sm text-blue-800 dark:text-blue-200">
                        <ChatBubbleLeftRightIcon className="h-4 w-4 inline mr-2" />
                        This discussion has {topic.interactionCount} {topic.interactionCount === 1 ? 'reply' : 'replies'}. 
                        Full conversation summary will be generated for high-engagement discussions.
                      </p>
                    </div>
                  )}

                  {/* Full Content */}
                  {topic.content && topic.content.trim().length > 0 ? (
                    <div className="mb-6">
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                          Full Content
                        </h3>
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          {topic.content.length.toLocaleString()} characters
                        </span>
                      </div>
                      <div className="prose dark:prose-invert max-w-none p-6 bg-gray-50 dark:bg-gray-900/50 rounded-xl border border-gray-200 dark:border-gray-700">
                        <div className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed font-mono text-sm">
                          {topic.content}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-900/50 border border-gray-200 dark:border-gray-700 rounded-lg">
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        Content extraction is in progress. Full content will be available once extraction completes.
                      </p>
                    </div>
                  )}

                  {/* Cluster Info */}
                  {clusterInfo && (
                    <div className="mb-6 p-4 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
                      <div className="flex items-center justify-between">
                        <div>
                          <h3 className="text-sm font-semibold text-blue-900 dark:text-blue-200 mb-1">
                            Part of a Cluster
                          </h3>
                          <p className="text-sm text-blue-700 dark:text-blue-300">
                            This topic is grouped with {clusterInfo.topicCount - 1} other related topic{clusterInfo.topicCount - 1 !== 1 ? 's' : ''}
                          </p>
                        </div>
                        <Link
                          href={`/topics/cluster/${clusterInfo.groupId}`}
                          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 dark:bg-blue-700 text-white hover:bg-blue-700 dark:hover:bg-blue-600 text-sm font-medium transition-colors"
                        >
                          View Cluster
                          <ArrowTopRightOnSquareIcon className="h-4 w-4" />
                        </Link>
                      </div>
                    </div>
                  )}

                  {/* Themes - Show hierarchical sub-themes */}
                  {topic.themes && topic.themes.length > 0 && (
                    <div className="mb-6">
                      <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-3">
                        Categories Detected by LLM:
                      </h3>
                      <div className="flex flex-wrap gap-2">
                        {topic.themes.map((theme) => {
                          // Display full path if theme has parent (e.g., "Kubernetes/Networking")
                          const displayName = theme.fullPath || theme.name
                          const isSubTheme = theme.parentTheme != null
                          
                          return (
                            <Link
                              key={theme.id}
                              href={`/topics/subtheme/${theme.id}`}
                              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-50 dark:bg-primary-900/20 text-primary-700 dark:text-primary-300 hover:bg-primary-100 dark:hover:bg-primary-900/30 text-sm font-medium transition-colors"
                            >
                              <TagIcon className="h-4 w-4" />
                              {displayName}
                              {isSubTheme && (
                                <span className="text-xs text-primary-500 dark:text-primary-400">
                                  (sub-category)
                                </span>
                              )}
                            </Link>
                          )
                        })}
                      </div>
                    </div>
                  )}

                  {/* Tags - Categorization tags */}
                  <div className="mb-6">
                    <div className="flex items-center justify-between mb-3">
                      <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300">
                        Tags:
                      </h3>
                      <button
                        onClick={handleRegenerateTags}
                        disabled={regeneratingTags}
                        className="inline-flex items-center gap-1 text-xs px-2 py-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors disabled:opacity-50"
                        title="Regenerate tags using AI"
                      >
                        {regeneratingTags ? (
                          <>
                            <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
                            Regenerating...
                          </>
                        ) : (
                          <>
                            <SparklesIcon className="h-3 w-3" />
                            Regenerate with AI
                          </>
                        )}
                      </button>
                    </div>
                    {tags.length > 0 ? (
                      <div className="flex flex-wrap gap-2">
                        {tags.map((tag, index) => (
                          <span
                            key={index}
                            className="inline-flex items-center gap-1 px-2 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-xs font-medium"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    ) : (
                      <p className="text-sm text-gray-500 dark:text-gray-400 italic">
                        No tags assigned. Click "Regenerate with AI" to generate tags.
                      </p>
                    )}
                  </div>

                  {/* Feedback Section */}
                  <div className="pt-6 border-t border-gray-200 dark:border-gray-700 mt-6">
                    <div className="flex items-center justify-between">
                      <div>
                        <h3 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                          Help Improve Content Quality
                        </h3>
                        <p className="text-xs text-gray-500 dark:text-gray-400 mb-4">
                          Your feedback helps us collect better topics in the future
                        </p>
                      </div>
                      <div className="flex items-center gap-3">
                        <button
                          onClick={handleThumbsUp}
                          disabled={feedbackLoading || topic.isRejected}
                          className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
                            topic.isRejected
                              ? 'bg-gray-100 dark:bg-gray-700 text-gray-400 cursor-not-allowed'
                              : 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300 hover:bg-green-100 dark:hover:bg-green-900/30'
                          } disabled:opacity-50`}
                          title="This topic is valuable and relevant"
                        >
                          {topic.thumbsUp && topic.thumbsUp > 0 ? (
                            <HandThumbUpIconSolid className="h-5 w-5" />
                          ) : (
                            <HandThumbUpIcon className="h-5 w-5" />
                          )}
                          {topic.thumbsUp || 0} Thumbs Up
                        </button>
                        <button
                          onClick={handleThumbsDown}
                          disabled={feedbackLoading || topic.isRejected}
                          className={`inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-all ${
                            topic.isRejected
                              ? 'bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 cursor-not-allowed'
                              : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 hover:bg-red-100 dark:hover:bg-red-900/30'
                          } disabled:opacity-50`}
                          title="This topic is not relevant or low quality"
                        >
                          {topic.thumbsDown && topic.thumbsDown > 0 ? (
                            <HandThumbDownIconSolid className="h-5 w-5" />
                          ) : (
                            <HandThumbDownIcon className="h-5 w-5" />
                          )}
                          {topic.thumbsDown || 0} Thumbs Down
                        </button>
                      </div>
                    </div>
                    {feedbackMessage && (
                      <div className={`mt-3 p-3 rounded-lg text-sm ${
                        feedbackMessage.includes('Thank you') || feedbackMessage.includes('improve')
                          ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
                          : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
                      }`}>
                        {feedbackMessage}
                      </div>
                    )}
                    {topic.isRejected && (
                      <div className="mt-3 p-3 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 text-sm">
                        This topic has been rejected and will be removed from the list.
                        {topic.rejectionReason && (
                          <p className="mt-1 text-xs">Reason: {topic.rejectionReason}</p>
                        )}
                      </div>
                    )}
                  </div>

                  {/* Original Source Link */}
                  <div className="pt-4 border-t border-gray-200 dark:border-gray-700 mt-6">
                    <a
                      href={topic.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-2 text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 font-medium transition-colors"
                    >
                      <ArrowTopRightOnSquareIcon className="h-5 w-5" />
                      View on {topic.source?.name || 'Original Source'}
                    </a>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* Related Topics from Various Sources */}
            {relatedTopics.length > 0 && (
              <motion.div variants={fadeInUp}>
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-4">
                  Related Discussions ({relatedTopics.length})
                </h2>
                <p className="text-gray-600 dark:text-gray-400 mb-6">
                  Other discussions about similar topics from various sources
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
                        {topics.map((relatedTopic) => (
                          <Link
                            key={relatedTopic.id}
                            href={`/topics/${relatedTopic.id}`}
                            className="block p-4 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-primary-300 dark:hover:border-primary-700 transition-colors group"
                          >
                            <div className="flex items-start justify-between gap-4">
                              <div className="flex-1 min-w-0">
                                <h4 className="text-base font-medium text-gray-900 dark:text-white mb-2 line-clamp-2 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors">
                                  {relatedTopic.title}
                                </h4>
                                
                                <div className="flex flex-wrap items-center gap-3 text-sm text-gray-500 dark:text-gray-400 mb-3">
                                  {relatedTopic.interactionCount != null && relatedTopic.interactionCount > 0 && (
                                    <span className="inline-flex items-center gap-1">
                                      <ChatBubbleLeftRightIcon className="h-4 w-4" />
                                      {relatedTopic.interactionCount} replies
                                    </span>
                                  )}
                                  {relatedTopic.viewCount != null && relatedTopic.viewCount > 0 && (
                                    <span className="inline-flex items-center gap-1">
                                      <EyeIcon className="h-4 w-4" />
                                      {relatedTopic.viewCount.toLocaleString()} views
                                    </span>
                                  )}
                                  {relatedTopic.score != null && relatedTopic.score > 0 && (
                                    <span className="inline-flex items-center gap-1">
                                      <StarIcon className="h-4 w-4" />
                                      Score: {relatedTopic.score}
                                    </span>
                                  )}
                                  {relatedTopic.engagementScore != null && relatedTopic.engagementScore > 0 && (
                                    <span className="inline-flex items-center gap-1 text-orange-600 dark:text-orange-400">
                                      <FireIcon className="h-4 w-4" />
                                      Engagement: {relatedTopic.engagementScore.toFixed(1)}
                                    </span>
                                  )}
                                </div>
                              </div>
                              
                              <div className="flex-shrink-0 flex items-center gap-2">
                                <a
                                  href={relatedTopic.url}
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
            )}

            {/* No Related Topics */}
            {relatedTopics.length === 0 && !loading && (
              <motion.div
                variants={fadeInUp}
                className="rounded-xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-8 text-center"
              >
                <p className="text-gray-600 dark:text-gray-400">
                  No related discussions found from other sources yet.
                </p>
              </motion.div>
            )}
          </>
        )}
      </motion.div>
    </Layout>
  )
}
