import React, { useState } from 'react'
import Head from 'next/head'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { 
  ChartBarIcon,
  TagIcon,
  ArrowRightIcon,
  SparklesIcon
} from '@heroicons/react/24/outline'
import Layout from '../components/Layout'
import apiClient from '../lib/api'
import { Theme } from '../types'

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  animate: { 
    opacity: 1, 
    y: 0,
    transition: { duration: 0.4, ease: [0.4, 0, 0.2, 1] }
  }
}

const colors = [
  'from-primary-500 to-primary-600',
  'from-accent-500 to-accent-600',
  'from-purple-500 to-purple-600',
  'from-blue-500 to-blue-600',
  'from-green-500 to-green-600',
  'from-yellow-500 to-yellow-600',
  'from-pink-500 to-pink-600',
  'from-indigo-500 to-indigo-600',
]

export default function ThemesPage() {
  const [themes, setThemes] = useState<Theme[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  React.useEffect(() => {
    const fetchThemes = async () => {
      try {
        setLoading(true)
        const themesData = await apiClient.getThemes()
        setThemes(themesData)
      } catch (err: any) {
        setError(err.message || 'Failed to load themes')
        console.error('Error fetching themes:', err)
      } finally {
        setLoading(false)
      }
    }

    fetchThemes()
  }, [])

  return (
    <Layout>
      <Head>
        <title>Themes - Cloud Native Scanner</title>
        <meta name="description" content="Browse all cloud native themes" />
      </Head>

      <motion.div
        initial="initial"
        animate="animate"
        variants={{ animate: { transition: { staggerChildren: 0.1 } } }}
        className="space-y-6"
      >
        {/* Header */}
        <motion.div variants={fadeInUp}>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Themes</h1>
          <p className="text-gray-600 dark:text-gray-400">
            Explore topics organized by themes and categories
          </p>
        </motion.div>

        {/* Loading State */}
        {loading && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary-200 border-t-primary-600"></div>
            <p className="mt-4 text-gray-600 dark:text-gray-400">Loading themes...</p>
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

        {/* Empty State */}
        {!loading && !error && themes.length === 0 && (
          <motion.div
            variants={fadeInUp}
            className="rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 p-12 text-center"
          >
            <SparklesIcon className="h-12 w-12 text-gray-400 dark:text-gray-500 mx-auto mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">
              No themes found
            </h3>
            <p className="text-gray-600 dark:text-gray-400">
              Themes will appear here once topics are analyzed and categorized.
            </p>
          </motion.div>
        )}

        {/* Themes Grid */}
        {!loading && !error && themes.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {themes.map((theme, index) => {
              const colorClass = colors[index % colors.length]
              return (
                <motion.div
                  key={theme.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  className="group relative overflow-hidden rounded-2xl bg-white dark:bg-gray-800 shadow-soft border border-gray-100 dark:border-gray-700 hover:shadow-soft-lg transition-all duration-300"
                >
                  {/* Gradient accent */}
                  <div className={`absolute top-0 left-0 right-0 h-1 bg-gradient-to-r ${colorClass}`} />
                  
                  <div className="p-6">
                    <div className={`inline-flex p-3 rounded-xl bg-gradient-to-br ${colorClass} mb-4`}>
                      <TagIcon className="h-6 w-6 text-white" />
                    </div>
                    
                    <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
                      {theme.name}
                    </h3>
                    
                    {theme.description && (
                      <p className="text-gray-600 dark:text-gray-400 text-sm mb-6 line-clamp-2">
                        {theme.description}
                      </p>
                    )}

                    <Link href={`/topics?themeId=${theme.id}`}>
                      <motion.div
                        whileHover={{ x: 4 }}
                        className="inline-flex items-center gap-2 text-sm font-medium text-primary-600 dark:text-primary-400 group-hover:text-primary-700 dark:group-hover:text-primary-300 transition-colors"
                      >
                        View Topics
                        <ArrowRightIcon className="h-4 w-4" />
                      </motion.div>
                    </Link>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}

        {/* Stats */}
        {!loading && !error && themes.length > 0 && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="rounded-xl bg-gradient-to-r from-primary-50 to-accent-50 dark:from-primary-900/20 dark:to-accent-900/20 p-6 border border-primary-100 dark:border-primary-800"
          >
            <div className="flex items-center gap-2 text-primary-700 dark:text-primary-300">
              <ChartBarIcon className="h-5 w-5" />
              <span className="font-semibold">{themes.length} themes available</span>
            </div>
          </motion.div>
        )}
      </motion.div>
    </Layout>
  )
}
