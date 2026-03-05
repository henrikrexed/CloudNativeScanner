import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { 
  MagnifyingGlassIcon, 
  ChartBarIcon, 
  CogIcon, 
  BellIcon,
  DocumentTextIcon,
  TagIcon,
  ArrowTrendingUpIcon,
  SparklesIcon,
  ArrowRightIcon,
  FireIcon
} from '@heroicons/react/24/outline'
import Layout from '../components/Layout'
import apiClient from '../lib/api'
import { DashboardStats, Topic } from '../types'
import { formatDate } from '../lib/dateUtils'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

const staggerContainer = {
  animate: {
    transition: {
      staggerChildren: 0.1,
      delayChildren: 0.1
    }
  }
}

const StatCard = ({ 
  title, 
  value, 
  icon: Icon, 
  color, 
  trend,
  delay = 0 
}: { 
  title: string
  value: string | number
  icon: any
  color: string
  trend?: string
  delay?: number
}) => (
  <motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.4, delay, ease: [0.4, 0, 0.2, 1] }}
    className="group relative overflow-hidden rounded-2xl bg-white dark:bg-gray-800 p-6 shadow-soft hover:shadow-soft-lg transition-all duration-300 border border-gray-100 dark:border-gray-700"
  >
    <div className="flex items-start justify-between">
      <div className="flex-1">
        <p className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">{title}</p>
        <p className="text-3xl font-bold text-gray-900 dark:text-white mb-2">{value}</p>
        {trend && (
          <div className="flex items-center text-sm">
            <ArrowTrendingUpIcon className="h-4 w-4 text-green-500 mr-1" />
            <span className="text-green-600 dark:text-green-400 font-medium">{trend}</span>
          </div>
        )}
      </div>
      <div className={`p-3 rounded-xl ${color} bg-opacity-10`}>
        <Icon className={`h-6 w-6 ${color.replace('bg-', 'text-')}`} />
      </div>
    </div>
    <div className={`absolute bottom-0 left-0 right-0 h-1 ${color} opacity-0 group-hover:opacity-100 transition-opacity`} />
  </motion.div>
)

interface ThemeStat {
  themeId: number
  themeName: string
  description: string
  totalTopics: number
  directTopicCount: number
  subThemeCount: number
  subThemes: Array<{
    subThemeId: number
    subThemeName: string
    description: string
    topicCount: number
  }>
}

interface ThemeStatsData {
  themes: ThemeStat[]
  totalThemes: number
  totalSubThemes: number
}

export default function Home() {
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [recentTopics, setRecentTopics] = useState<Topic[]>([])
  const [themeStats, setThemeStats] = useState<ThemeStatsData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true)
        const [statsData, topicsData, themeStatsData] = await Promise.all([
          apiClient.getDashboardStats().catch(() => null),
          apiClient.getHottestTopics(5).catch(() => []), // Show hottest topics instead of recent
          apiClient.getThemeStats().catch(() => null)
        ])
        setStats(statsData)
        setRecentTopics(topicsData)
        setThemeStats(themeStatsData)
      } catch (err: any) {
        setError(err.message || 'Failed to load dashboard data')
        console.error('Error fetching dashboard data:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [])

  return (
    <Layout>
      <Head>
        <title>Dashboard - Cloud Native Scanner</title>
        <meta name="description" content="Monitor and analyze cloud native topics from various sources" />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={staggerContainer}
        className="space-y-6"
      >
        {/* Welcome Header */}
        <motion.div variants={fadeInUp} className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">
            Welcome back! 👋
          </h1>
          <p className="text-gray-600 dark:text-gray-400">
            Here's what's happening with your cloud native topics today.
          </p>
        </motion.div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            title="Total Topics"
            value={loading ? '...' : stats?.totalTopics?.toLocaleString() || '0'}
            icon={DocumentTextIcon}
            color="text-primary-600 bg-primary-600"
            trend="+12% from last week"
            delay={0}
          />
          <StatCard
            title="Active Sources"
            value={loading ? '...' : stats?.activeSources || '0'}
            icon={SparklesIcon}
            color="text-accent-600 bg-accent-600"
            delay={0.1}
          />
          <StatCard
            title="New Today"
            value={loading ? '...' : stats?.newTopicsToday || '0'}
            icon={ArrowTrendingUpIcon}
            color="text-green-600 bg-green-600"
            trend="+5 today"
            delay={0.2}
          />
          <StatCard
            title="Themes"
            value={loading ? '...' : stats?.totalThemes || '0'}
            icon={TagIcon}
            color="text-purple-600 bg-purple-600"
            delay={0.3}
          />
        </div>

        {/* Theme Statistics */}
        {themeStats && themeStats.themes && themeStats.themes.length > 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 overflow-hidden"
          >
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                  <ChartBarIcon className="h-5 w-5 text-primary-600 dark:text-primary-400" />
                  Topics by Theme & Sub-Theme
                </h2>
                <Link href="/themes" className="text-sm font-medium text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300">
                  View all themes
                </Link>
              </div>
            </div>
            <div className="p-6">
              <div className="space-y-6">
                {themeStats.themes.map((theme, themeIndex) => (
                  <motion.div
                    key={theme.themeId}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: themeIndex * 0.1 }}
                    className="border border-gray-200 dark:border-gray-700 rounded-xl p-5 hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="text-base font-semibold text-gray-900 dark:text-white">
                            {theme.themeName}
                          </h3>
                          {theme.totalTopics > 0 && (
                            <Link
                              href={`/topics?themeId=${theme.themeId}`}
                              className="inline-flex items-center gap-1 text-xs font-medium text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300 transition-colors"
                            >
                              <ArrowRightIcon className="h-3 w-3" />
                              View topics
                            </Link>
                          )}
                        </div>
                        {theme.description && (
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {theme.description}
                          </p>
                        )}
                      </div>
                      <Link
                        href={`/topics?themeId=${theme.themeId}`}
                        className="ml-4 text-right hover:opacity-80 transition-opacity"
                      >
                        <div className="text-2xl font-bold text-primary-600 dark:text-primary-400">
                          {theme.totalTopics}
                        </div>
                        <div className="text-xs text-gray-500 dark:text-gray-500">
                          topics
                        </div>
                      </Link>
                    </div>
                    
                    {theme.subThemes && theme.subThemes.length > 0 && (
                      <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                          {theme.subThemes.map((subTheme) => (
                            <Link
                              key={subTheme.subThemeId}
                              href={`/topics/subtheme/${subTheme.subThemeId}`}
                              className="group rounded-lg bg-gray-50 dark:bg-gray-700/50 p-3 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition-colors"
                            >
                              <div className="flex items-center justify-between">
                                <div className="flex-1 min-w-0">
                                  <h4 className="text-sm font-medium text-gray-900 dark:text-white group-hover:text-primary-600 dark:group-hover:text-primary-400 truncate">
                                    {subTheme.subThemeName}
                                  </h4>
                                  {subTheme.description && (
                                    <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5 truncate">
                                      {subTheme.description}
                                    </p>
                                  )}
                                </div>
                                <div className="ml-2 flex-shrink-0">
                                  <span className="inline-flex items-center justify-center w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 text-xs font-semibold">
                                    {subTheme.topicCount}
                                  </span>
                                </div>
                              </div>
                            </Link>
                          ))}
                        </div>
                        {theme.directTopicCount > 0 && (
                          <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
                            <div className="text-xs text-gray-500 dark:text-gray-400">
                              <span className="font-medium">{theme.directTopicCount}</span> topics directly in {theme.themeName}
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                    
                    {(!theme.subThemes || theme.subThemes.length === 0) && theme.directTopicCount > 0 && (
                      <div className="mt-3 text-sm text-gray-600 dark:text-gray-400">
                        {theme.directTopicCount} topic{theme.directTopicCount !== 1 ? 's' : ''} in this theme
                      </div>
                    )}
                  </motion.div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* Quick Actions */}
        <motion.div variants={fadeInUp} className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Link href="/search">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="group relative overflow-hidden rounded-xl bg-gradient-to-br from-primary-500 to-primary-600 p-6 text-white shadow-lg hover:shadow-xl transition-all duration-300"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold mb-1">Search Topics</h3>
                  <p className="text-primary-100 text-sm">Find specific topics and insights</p>
                </div>
                <MagnifyingGlassIcon className="h-8 w-8 text-primary-200" />
              </div>
              <ArrowRightIcon className="absolute bottom-4 right-4 h-5 w-5 text-primary-200 group-hover:translate-x-1 transition-transform" />
            </motion.div>
          </Link>

          <Link href="/topics">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="group relative overflow-hidden rounded-xl bg-gradient-to-br from-accent-500 to-accent-600 p-6 text-white shadow-lg hover:shadow-xl transition-all duration-300"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold mb-1">Browse Topics</h3>
                  <p className="text-accent-100 text-sm">Explore all topics</p>
                </div>
                <DocumentTextIcon className="h-8 w-8 text-accent-200" />
              </div>
              <ArrowRightIcon className="absolute bottom-4 right-4 h-5 w-5 text-accent-200 group-hover:translate-x-1 transition-transform" />
            </motion.div>
          </Link>

          <Link href="/themes">
            <motion.div
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="group relative overflow-hidden rounded-xl bg-gradient-to-br from-purple-500 to-purple-600 p-6 text-white shadow-lg hover:shadow-xl transition-all duration-300"
            >
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold mb-1">View Themes</h3>
                  <p className="text-purple-100 text-sm">Analyze topic themes</p>
                </div>
                <ChartBarIcon className="h-8 w-8 text-purple-200" />
              </div>
              <ArrowRightIcon className="absolute bottom-4 right-4 h-5 w-5 text-purple-200 group-hover:translate-x-1 transition-transform" />
            </motion.div>
          </Link>
        </motion.div>

        {/* Recent Topics */}
        {recentTopics.length > 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 overflow-hidden"
          >
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                  <FireIcon className="h-5 w-5 text-orange-500" />
                  Hottest Topics
                </h2>
                <Link href="/topics" className="text-sm font-medium text-primary-600 dark:text-primary-400 hover:text-primary-700 dark:hover:text-primary-300">
                  View all
                </Link>
              </div>
            </div>
            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {recentTopics.map((topic, index) => (
                <motion.div
                  key={topic.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: index * 0.1 }}
                  className="px-6 py-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors"
                >
                  <Link href={`/topics/${topic.id}`} className="block group">
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <h3 className="text-sm font-semibold text-gray-900 dark:text-white group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors truncate">
                          {topic.title}
                        </h3>
                        <div className="mt-1 flex items-center gap-3 text-xs text-gray-500 dark:text-gray-400">
                          {topic.primaryTheme && (
                            <span className="inline-flex items-center gap-1">
                              <TagIcon className="h-3 w-3" />
                              {topic.primaryTheme.name}
                            </span>
                          )}
                          {topic.source && (
                            <span>{topic.source.name}</span>
                          )}
                          {topic.engagementScore != null && topic.engagementScore > 0 && (
                            <span className="inline-flex items-center gap-1 text-orange-600 dark:text-orange-400 font-medium">
                              <FireIcon className="h-3 w-3" />
                              {topic.engagementScore.toFixed(1)}
                            </span>
                          )}
                          {topic.interactionCount != null && topic.interactionCount > 0 && (
                            <span>💬 {topic.interactionCount}</span>
                          )}
                          {topic.createdAt && (
                            <span>{formatDate(topic.createdAt)}</span>
                          )}
                        </div>
                      </div>
                      <ArrowRightIcon className="h-5 w-5 text-gray-400 group-hover:text-primary-600 dark:group-hover:text-primary-400 transition-colors flex-shrink-0 ml-4" />
                    </div>
                  </Link>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

        {/* Error State */}
        {error && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="rounded-xl bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-800 p-4"
          >
            <div className="flex items-start">
              <BellIcon className="h-5 w-5 text-yellow-600 dark:text-yellow-400 mt-0.5 mr-3 flex-shrink-0" />
              <div>
                <h3 className="text-sm font-medium text-yellow-800 dark:text-yellow-200">
                  Unable to load dashboard data
                </h3>
                <p className="mt-1 text-sm text-yellow-700 dark:text-yellow-300">
                  Some features may be limited. Please check your connection and try again.
                </p>
              </div>
            </div>
          </motion.div>
        )}

        {/* Empty State */}
        {!loading && !error && recentTopics.length === 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <SparklesIcon className="h-12 w-12 text-gray-400 dark:text-gray-500 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              No topics yet
            </h3>
            <p className="text-gray-600 dark:text-gray-400 mb-6 max-w-sm mx-auto">
              Start scanning to discover cloud native topics from various sources.
            </p>
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
          </motion.div>
        )}
      </motion.div>
    </Layout>
  )
}
