import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import Layout from '../../components/Layout'
import apiClient from '../../lib/api'
import {
  ChartBarIcon,
  DocumentTextIcon,
  CubeIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  XCircleIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline'
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts'

interface MonitoringMetrics {
  summary: {
    totalTopics: number
    totalSources: number
    activeSources: number
    totalThemes: number
    totalClusters: number
    contentExtractionCompleted: number
    contentExtractionPending: number
    contentExtractionFailed: number
  }
  topicsBySource: Array<{
    sourceId: number
    sourceName: string
    topicCount: number
    enabled: boolean
  }>
  topicsByTheme: Array<{
    themeId: number
    themeName: string
    topicCount: number
    parentThemeId: number | null
    hasKeywords?: boolean
  }>
  themesWithoutKeywords: Array<{
    themeId: number
    themeName: string
    topicCount: number
  }>
  themesWithoutKeywordsCount: number
  contentExtractionStatus: {
    PENDING: number
    PROCESSING: number
    COMPLETED: number
    FAILED: number
  }
  contentExtractionBySource: Array<{
    sourceId: number
    sourceName: string
    pending: number
    processing: number
    completed: number
    failed: number
  }>
  clusterStatistics: {
    totalClusters: number
    totalTopicsInClusters: number
    averageTopicsPerCluster: number
  }
  ragStatistics: {
    topicsWithSummary: number
    topicsInRAG: number
  }
  errorStatistics: {
    failedContentExtraction: number
    topicsWithoutThemes: number
    totalErrors: number
  }
}

export default function MonitoringPage() {
  const [metrics, setMetrics] = useState<MonitoringMetrics | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  const fetchMetrics = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.getMonitoringMetrics()
      setMetrics(data)
      setLastUpdated(new Date())
    } catch (err: any) {
      console.error('Error fetching monitoring metrics:', err)
      setError(err.response?.data?.error || 'Failed to fetch monitoring metrics')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchMetrics()
    const interval = setInterval(fetchMetrics, 60000) // Refresh every minute
    return () => clearInterval(interval)
  }, [])

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat().format(num)
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'text-green-600 bg-green-50'
      case 'PROCESSING':
        return 'text-blue-600 bg-blue-50'
      case 'PENDING':
        return 'text-yellow-600 bg-yellow-50'
      case 'FAILED':
        return 'text-red-600 bg-red-50'
      default:
        return 'text-gray-600 bg-gray-50'
    }
  }

  if (loading && !metrics) {
    return (
      <Layout>
        <Head>
          <title>Monitoring - Cloud Native Scanner</title>
        </Head>
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <ArrowPathIcon className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-4" />
            <p className="text-gray-600">Loading monitoring metrics...</p>
          </div>
        </div>
      </Layout>
    )
  }

  if (error && !metrics) {
    return (
      <Layout>
        <Head>
          <title>Monitoring - Cloud Native Scanner</title>
        </Head>
        <div className="space-y-6">
          <h1 className="text-3xl font-bold text-gray-900">System Monitoring</h1>
          <div className="bg-red-50 border border-red-200 rounded-lg p-4">
            <div className="flex items-center">
              <XCircleIcon className="h-5 w-5 text-red-600 mr-2" />
              <p className="text-red-800">{error}</p>
            </div>
            <button
              onClick={fetchMetrics}
              className="mt-4 px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700"
            >
              Retry
            </button>
          </div>
        </div>
      </Layout>
    )
  }

  if (!metrics) return null

  return (
    <Layout>
      <Head>
        <title>Monitoring - Cloud Native Scanner</title>
        <meta name="description" content="System monitoring and metrics dashboard" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold text-gray-900">System Monitoring</h1>
          <div className="flex items-center gap-4">
            {lastUpdated && (
              <span className="text-sm text-gray-500">
                Last updated: {lastUpdated.toLocaleTimeString()}
              </span>
            )}
            <button
              onClick={fetchMetrics}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              <ArrowPathIcon className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Topics</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">
                  {formatNumber(metrics.summary.totalTopics)}
                </p>
              </div>
              <DocumentTextIcon className="h-8 w-8 text-blue-600" />
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Active Sources</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">
                  {formatNumber(metrics.summary.activeSources)} / {formatNumber(metrics.summary.totalSources)}
                </p>
              </div>
              <ChartBarIcon className="h-8 w-8 text-green-600" />
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Content Extracted</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">
                  {formatNumber(metrics.summary.contentExtractionCompleted)}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {metrics.summary.contentExtractionPending} pending
                </p>
              </div>
              <CheckCircleIcon className="h-8 w-8 text-green-600" />
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Clusters</p>
                <p className="text-2xl font-bold text-gray-900 mt-1">
                  {formatNumber(metrics.clusterStatistics.totalClusters)}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {formatNumber(metrics.clusterStatistics.totalTopicsInClusters)} topics
                </p>
              </div>
              <CubeIcon className="h-8 w-8 text-purple-600" />
            </div>
          </div>
        </div>

        {/* Content Extraction Dashboard */}
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-2xl font-semibold text-gray-900">Content Extraction Dashboard</h2>
            <span className="text-sm text-gray-500">
              Total Topics: {formatNumber(
                metrics.contentExtractionStatus.PENDING +
                metrics.contentExtractionStatus.PROCESSING +
                metrics.contentExtractionStatus.COMPLETED +
                metrics.contentExtractionStatus.FAILED
              )}
            </span>
          </div>

          {/* Status Summary Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            {Object.entries(metrics.contentExtractionStatus).map(([status, count]) => {
              const total = metrics.contentExtractionStatus.PENDING +
                           metrics.contentExtractionStatus.PROCESSING +
                           metrics.contentExtractionStatus.COMPLETED +
                           metrics.contentExtractionStatus.FAILED
              const percentage = total > 0 ? ((count / total) * 100).toFixed(1) : 0
              const statusIcon = status === 'COMPLETED' ? CheckCircleIcon :
                                status === 'PROCESSING' ? ArrowPathIcon :
                                status === 'PENDING' ? ClockIcon : XCircleIcon
              const Icon = statusIcon
              
              return (
                <div key={status} className={`rounded-lg p-4 ${getStatusColor(status)} border-2`}>
                  <div className="flex items-center justify-between mb-2">
                    <Icon className={`h-5 w-5 ${status === 'PROCESSING' ? 'animate-spin' : ''}`} />
                    <span className="text-xs font-medium opacity-75">{percentage}%</span>
                  </div>
                  <p className="text-sm font-medium mb-1">{status}</p>
                  <p className="text-2xl font-bold">{formatNumber(count)}</p>
                  {/* Progress bar */}
                  <div className="mt-2 w-full bg-gray-200 rounded-full h-2">
                    <div
                      className={`h-2 rounded-full ${
                        status === 'COMPLETED' ? 'bg-green-600' :
                        status === 'PROCESSING' ? 'bg-blue-600' :
                        status === 'PENDING' ? 'bg-yellow-600' : 'bg-red-600'
                      }`}
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              )
            })}
          </div>

          {/* Charts Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            {/* Pie Chart */}
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Status Distribution</h3>
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={[
                      { name: 'Completed', value: metrics.contentExtractionStatus.COMPLETED },
                      { name: 'Pending', value: metrics.contentExtractionStatus.PENDING },
                      { name: 'Processing', value: metrics.contentExtractionStatus.PROCESSING },
                      { name: 'Failed', value: metrics.contentExtractionStatus.FAILED }
                    ]}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    <Cell fill="#10b981" />
                    <Cell fill="#eab308" />
                    <Cell fill="#3b82f6" />
                    <Cell fill="#ef4444" />
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Bar Chart by Source */}
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Status by Source</h3>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart
                  data={metrics.contentExtractionBySource
                    .filter(s => s.pending + s.processing + s.completed + s.failed > 0)
                    .map(source => ({
                      name: source.sourceName,
                      Completed: source.completed,
                      Pending: source.pending,
                      Processing: source.processing,
                      Failed: source.failed
                    }))}
                  margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
                >
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" angle={-45} textAnchor="end" height={100} />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="Completed" stackId="a" fill="#10b981" />
                  <Bar dataKey="Processing" stackId="a" fill="#3b82f6" />
                  <Bar dataKey="Pending" stackId="a" fill="#eab308" />
                  <Bar dataKey="Failed" stackId="a" fill="#ef4444" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          {/* Summary Statistics */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6 p-4 bg-gray-50 rounded-lg">
            <div className="text-center">
              <p className="text-sm text-gray-600 mb-1">Completion Rate</p>
              <p className="text-2xl font-bold text-green-600">
                {(() => {
                  const total = metrics.contentExtractionStatus.PENDING +
                               metrics.contentExtractionStatus.PROCESSING +
                               metrics.contentExtractionStatus.COMPLETED +
                               metrics.contentExtractionStatus.FAILED
                  return total > 0
                    ? ((metrics.contentExtractionStatus.COMPLETED / total) * 100).toFixed(1)
                    : 0
                })()}%
              </p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-600 mb-1">Failure Rate</p>
              <p className="text-2xl font-bold text-red-600">
                {(() => {
                  const total = metrics.contentExtractionStatus.PENDING +
                               metrics.contentExtractionStatus.PROCESSING +
                               metrics.contentExtractionStatus.COMPLETED +
                               metrics.contentExtractionStatus.FAILED
                  return total > 0
                    ? ((metrics.contentExtractionStatus.FAILED / total) * 100).toFixed(1)
                    : 0
                })()}%
              </p>
            </div>
            <div className="text-center">
              <p className="text-sm text-gray-600 mb-1">In Progress</p>
              <p className="text-2xl font-bold text-blue-600">
                {formatNumber(
                  metrics.contentExtractionStatus.PENDING +
                  metrics.contentExtractionStatus.PROCESSING
                )}
              </p>
            </div>
          </div>
        </div>

        {/* Topics by Source */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Topics by Source</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Source
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Topics
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {metrics.topicsBySource.map((source) => (
                  <tr key={source.sourceId}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {source.sourceName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 text-xs font-semibold rounded-full ${
                        source.enabled ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                      }`}>
                        {source.enabled ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatNumber(source.topicCount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Content Extraction by Source - Enhanced Table */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Content Extraction by Source</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Source
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Total
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Pending
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Processing
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Completed
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Failed
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Progress
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {metrics.contentExtractionBySource
                  .filter(source => source.pending + source.processing + source.completed + source.failed > 0)
                  .map((source) => {
                    const total = source.pending + source.processing + source.completed + source.failed
                    const completedPercent = total > 0 ? ((source.completed / total) * 100).toFixed(1) : 0
                    const failedPercent = total > 0 ? ((source.failed / total) * 100).toFixed(1) : 0
                    
                    return (
                      <tr key={source.sourceId} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          {source.sourceName}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                          {formatNumber(total)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className="text-sm text-yellow-600 font-medium">
                              {formatNumber(source.pending)}
                            </span>
                            {total > 0 && (
                              <span className="ml-2 text-xs text-gray-500">
                                ({((source.pending / total) * 100).toFixed(0)}%)
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className="text-sm text-blue-600 font-medium">
                              {formatNumber(source.processing)}
                            </span>
                            {total > 0 && (
                              <span className="ml-2 text-xs text-gray-500">
                                ({((source.processing / total) * 100).toFixed(0)}%)
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className="text-sm text-green-600 font-medium">
                              {formatNumber(source.completed)}
                            </span>
                            {total > 0 && (
                              <span className="ml-2 text-xs text-gray-500">
                                ({completedPercent}%)
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <span className="text-sm text-red-600 font-medium">
                              {formatNumber(source.failed)}
                            </span>
                            {total > 0 && (
                              <span className="ml-2 text-xs text-gray-500">
                                ({failedPercent}%)
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="w-32">
                            <div className="w-full bg-gray-200 rounded-full h-3 flex overflow-hidden">
                              <div
                                className="bg-green-600"
                                style={{ width: `${completedPercent}%` }}
                                title={`Completed: ${completedPercent}%`}
                              />
                              <div
                                className="bg-blue-600"
                                style={{ width: `${total > 0 ? ((source.processing / total) * 100).toFixed(1) : 0}%` }}
                                title={`Processing: ${total > 0 ? ((source.processing / total) * 100).toFixed(1) : 0}%`}
                              />
                              <div
                                className="bg-yellow-600"
                                style={{ width: `${total > 0 ? ((source.pending / total) * 100).toFixed(1) : 0}%` }}
                                title={`Pending: ${total > 0 ? ((source.pending / total) * 100).toFixed(1) : 0}%`}
                              />
                              <div
                                className="bg-red-600"
                                style={{ width: `${failedPercent}%` }}
                                title={`Failed: ${failedPercent}%`}
                              />
                            </div>
                            <p className="text-xs text-gray-500 mt-1">
                              {completedPercent}% Complete
                            </p>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Topics by Theme (Top 10) */}
        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Topics by Theme (Top 10)</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Theme
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Topics
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {metrics.topicsByTheme.slice(0, 10).map((theme) => (
                  <tr key={theme.themeId}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {theme.themeName}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatNumber(theme.topicCount)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Cluster and RAG Statistics */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Cluster Statistics</h2>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Total Clusters</span>
                <span className="text-lg font-semibold text-gray-900">
                  {formatNumber(metrics.clusterStatistics.totalClusters)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Topics in Clusters</span>
                <span className="text-lg font-semibold text-gray-900">
                  {formatNumber(metrics.clusterStatistics.totalTopicsInClusters)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Avg Topics per Cluster</span>
                <span className="text-lg font-semibold text-gray-900">
                  {metrics.clusterStatistics.averageTopicsPerCluster.toFixed(1)}
                </span>
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">RAG Storage Statistics</h2>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Topics with Summary</span>
                <span className="text-lg font-semibold text-gray-900">
                  {formatNumber(metrics.ragStatistics.topicsWithSummary)}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm text-gray-600">Topics in RAG</span>
                <span className="text-lg font-semibold text-gray-900">
                  {formatNumber(metrics.ragStatistics.topicsInRAG)}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Themes Without Keywords Warning */}
        {metrics.themesWithoutKeywordsCount > 0 && (
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center">
                <ExclamationTriangleIcon className="h-6 w-6 text-yellow-600 mr-2" />
                <h2 className="text-xl font-semibold text-yellow-900">
                  Themes Without Keywords ({metrics.themesWithoutKeywordsCount})
                </h2>
              </div>
            </div>
            <p className="text-sm text-yellow-800 mb-4">
              These enabled themes don't have search keywords configured. Scanners will use fallback keyword generation, 
              but results may be limited. Consider generating keywords for better search coverage.
            </p>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-yellow-200">
                <thead className="bg-yellow-100">
                  <tr>
                    <th className="px-4 py-2 text-left text-xs font-medium text-yellow-800 uppercase">
                      Theme Name
                    </th>
                    <th className="px-4 py-2 text-left text-xs font-medium text-yellow-800 uppercase">
                      Topics
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-yellow-200">
                  {metrics.themesWithoutKeywords.slice(0, 10).map((theme) => (
                    <tr key={theme.themeId}>
                      <td className="px-4 py-2 text-sm font-medium text-yellow-900">
                        {theme.themeName}
                      </td>
                      <td className="px-4 py-2 text-sm text-yellow-700">
                        {formatNumber(theme.topicCount)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {metrics.themesWithoutKeywordsCount > 10 && (
              <p className="text-xs text-yellow-700 mt-2">
                Showing first 10 of {metrics.themesWithoutKeywordsCount} themes without keywords
              </p>
            )}
          </div>
        )}

        {/* Error Statistics */}
        {metrics.errorStatistics.totalErrors > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-6">
            <div className="flex items-center mb-4">
              <ExclamationTriangleIcon className="h-6 w-6 text-red-600 mr-2" />
              <h2 className="text-xl font-semibold text-red-900">Error Statistics</h2>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <p className="text-sm text-red-700 mb-1">Failed Content Extraction</p>
                <p className="text-2xl font-bold text-red-900">
                  {formatNumber(metrics.errorStatistics.failedContentExtraction)}
                </p>
              </div>
              <div>
                <p className="text-sm text-red-700 mb-1">Topics Without Themes</p>
                <p className="text-2xl font-bold text-red-900">
                  {formatNumber(metrics.errorStatistics.topicsWithoutThemes)}
                </p>
              </div>
              <div>
                <p className="text-sm text-red-700 mb-1">Total Errors</p>
                <p className="text-2xl font-bold text-red-900">
                  {formatNumber(metrics.errorStatistics.totalErrors)}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </Layout>
  )
}
