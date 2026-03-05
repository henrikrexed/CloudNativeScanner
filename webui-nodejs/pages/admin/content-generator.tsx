import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import Layout from '../../components/Layout'
import apiClient from '../../lib/api'
import { ContentGenerationRequest, ContentGenerationResponse, PersonalContent } from '../../types'
import { 
  SparklesIcon,
  DocumentTextIcon,
  VideoCameraIcon,
  NewspaperIcon,
  ArrowPathIcon
} from '@heroicons/react/24/outline'

export default function ContentGeneratorPage() {
  const [prompt, setPrompt] = useState('')
  const [contentType, setContentType] = useState<'YOUTUBE_STORYBOARD' | 'BLOG_POST' | 'ARTICLE'>('BLOG_POST')
  const [category, setCategory] = useState('')
  const [tags, setTags] = useState<string[]>([])
  const [tagInput, setTagInput] = useState('')
  const [usePersonalStyle, setUsePersonalStyle] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [generatedContent, setGeneratedContent] = useState<ContentGenerationResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [streamingContent, setStreamingContent] = useState<string>('')
  const [personalContentStats, setPersonalContentStats] = useState<{ total: number; inRag: number } | null>(null)

  useEffect(() => {
    fetchPersonalContentStats()
  }, [])

  const fetchPersonalContentStats = async () => {
    try {
      const data = await apiClient.getPersonalContent()
      const inRag = data.content.filter(c => c.ragStored).length
      setPersonalContentStats({ total: data.total, inRag })
    } catch (err) {
      console.error('Failed to fetch personal content stats:', err)
    }
  }

  const handleAddTag = () => {
    if (tagInput.trim() && !tags.includes(tagInput.trim())) {
      setTags([...tags, tagInput.trim()])
      setTagInput('')
    }
  }

  const handleRemoveTag = (tag: string) => {
    setTags(tags.filter(t => t !== tag))
  }

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!prompt.trim()) {
      setError('Please enter a prompt')
      return
    }

    setGenerating(true)
    setError(null)
    setGeneratedContent(null)
    setStreamingContent('')

    try {
      const request: ContentGenerationRequest = {
        prompt: prompt.trim(),
        contentType,
        category: category.trim() || undefined,
        tags: tags.length > 0 ? tags : undefined,
        usePersonalStyle
      }

      // Use streaming API to prevent timeouts
      await apiClient.generateContentStream(
        request,
        (chunk: string) => {
          // Append chunk as it arrives
          setStreamingContent(prev => prev + chunk)
        },
        (response: ContentGenerationResponse) => {
          // Stream completed successfully
          setGeneratedContent(response)
          setStreamingContent('')
        },
        (errorMsg: string) => {
          setError(errorMsg)
        }
      )
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to generate content'
      setError(errorMessage)
    } finally {
      setGenerating(false)
    }
  }

  const handleCopy = () => {
    if (generatedContent?.generatedContent) {
      navigator.clipboard.writeText(generatedContent.generatedContent)
      alert('Content copied to clipboard!')
    }
  }

  const handleDownload = () => {
    if (generatedContent?.generatedContent) {
      const blob = new Blob([generatedContent.generatedContent], { type: 'text/plain' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `generated-${contentType.toLowerCase()}-${Date.now()}.txt`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
    }
  }

  return (
    <Layout>
      <Head>
        <title>Content Generator - Admin - Cloud Native Scanner</title>
        <meta name="description" content="Generate YouTube storyboards and blog posts based on your personal content style" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <SparklesIcon className="h-8 w-8 text-gray-600 dark:text-gray-400" />
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Content Generator</h1>
        </div>

        {personalContentStats && (
          <div className="bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg p-4">
            <p className="text-sm text-blue-800 dark:text-blue-200">
              <strong>{personalContentStats.inRag}</strong> of <strong>{personalContentStats.total}</strong> personal content items are stored in RAG.
              {personalContentStats.inRag === 0 && ' Add personal content first to enable style-based generation.'}
            </p>
          </div>
        )}

        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-200 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Input Form */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-4">Generate Content</h2>
            
            <form onSubmit={handleGenerate} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Content Type *
                </label>
                <div className="grid grid-cols-3 gap-2">
                  <button
                    type="button"
                    onClick={() => setContentType('YOUTUBE_STORYBOARD')}
                    className={`flex flex-col items-center gap-2 p-3 border-2 rounded-lg transition-colors ${
                      contentType === 'YOUTUBE_STORYBOARD'
                        ? 'border-blue-600 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
                    }`}
                  >
                    <VideoCameraIcon className="h-6 w-6" />
                    <span className="text-xs font-medium">YouTube</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setContentType('BLOG_POST')}
                    className={`flex flex-col items-center gap-2 p-3 border-2 rounded-lg transition-colors ${
                      contentType === 'BLOG_POST'
                        ? 'border-blue-600 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
                    }`}
                  >
                    <DocumentTextIcon className="h-6 w-6" />
                    <span className="text-xs font-medium">Blog</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setContentType('ARTICLE')}
                    className={`flex flex-col items-center gap-2 p-3 border-2 rounded-lg transition-colors ${
                      contentType === 'ARTICLE'
                        ? 'border-blue-600 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-gray-300 dark:border-gray-600 hover:border-gray-400'
                    }`}
                  >
                    <NewspaperIcon className="h-6 w-6" />
                    <span className="text-xs font-medium">Article</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Prompt *
                </label>
                <textarea
                  value={prompt}
                  onChange={(e) => setPrompt(e.target.value)}
                  required
                  rows={6}
                  placeholder="Describe what you want to generate. For example: 'A tutorial on Kubernetes networking for beginners' or 'A blog post about microservices architecture'"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Category (optional)
                </label>
                <input
                  type="text"
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  placeholder="e.g., Tutorial, Documentation"
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Tags (optional)
                </label>
                <div className="flex gap-2 mb-2">
                  <input
                    type="text"
                    value={tagInput}
                    onChange={(e) => setTagInput(e.target.value)}
                    onKeyPress={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        handleAddTag()
                      }
                    }}
                    placeholder="Add a tag and press Enter"
                    className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                  />
                  <button
                    type="button"
                    onClick={handleAddTag}
                    className="px-4 py-2 bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-300 dark:hover:bg-gray-600"
                  >
                    Add
                  </button>
                </div>
                {tags.length > 0 && (
                  <div className="flex flex-wrap gap-2">
                    {tags.map((tag, idx) => (
                      <span
                        key={idx}
                        className="px-3 py-1 bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded-lg flex items-center gap-2"
                      >
                        {tag}
                        <button
                          type="button"
                          onClick={() => handleRemoveTag(tag)}
                          className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200"
                        >
                          ×
                        </button>
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="usePersonalStyle"
                  checked={usePersonalStyle}
                  onChange={(e) => setUsePersonalStyle(e.target.checked)}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="usePersonalStyle" className="ml-2 text-sm text-gray-700 dark:text-gray-300">
                  Use my personal writing style (based on stored content in RAG)
                </label>
              </div>

              <button
                type="submit"
                disabled={generating || !prompt.trim()}
                className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                {generating ? (
                  <>
                    <ArrowPathIcon className="h-5 w-5 animate-spin" />
                    Generating...
                  </>
                ) : (
                  <>
                    <SparklesIcon className="h-5 w-5" />
                    Generate Content
                  </>
                )}
              </button>
            </form>
          </div>

          {/* Generated Content */}
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Generated Content</h2>
              {generatedContent && (
                <div className="flex gap-2">
                  <button
                    onClick={handleCopy}
                    className="px-3 py-1 text-sm bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-300 dark:hover:bg-gray-600"
                  >
                    Copy
                  </button>
                  <button
                    onClick={handleDownload}
                    className="px-3 py-1 text-sm bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-300 dark:hover:bg-gray-600"
                  >
                    Download
                  </button>
                </div>
              )}
            </div>

            {generatedContent ? (
              <div className="space-y-4">
                {generatedContent.metadata && (
                  <div className="bg-gray-50 dark:bg-gray-700 rounded-lg p-4 text-sm">
                    {generatedContent.metadata.wordCount && (
                      <p className="text-gray-600 dark:text-gray-300">
                        <strong>Word Count:</strong> {generatedContent.metadata.wordCount}
                      </p>
                    )}
                    {generatedContent.metadata.estimatedDuration && (
                      <p className="text-gray-600 dark:text-gray-300">
                        <strong>Estimated Duration:</strong> {generatedContent.metadata.estimatedDuration} minutes
                      </p>
                    )}
                    {generatedContent.metadata.sections && generatedContent.metadata.sections.length > 0 && (
                      <div className="mt-2">
                        <strong className="text-gray-700 dark:text-gray-200">Sections:</strong>
                        <ul className="list-disc list-inside mt-1 text-gray-600 dark:text-gray-300">
                          {generatedContent.metadata.sections.map((section, idx) => (
                            <li key={idx}>{section}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>
                )}
                <div className="prose dark:prose-invert max-w-none">
                  <pre className="whitespace-pre-wrap text-sm text-gray-800 dark:text-gray-200 bg-gray-50 dark:bg-gray-900 p-4 rounded-lg border border-gray-200 dark:border-gray-700">
                    {generating && streamingContent ? streamingContent : generatedContent?.generatedContent || ''}
                    {generating && streamingContent && (
                      <span className="animate-pulse">▊</span>
                    )}
                  </pre>
                </div>
              </div>
            ) : (
              <div className="text-center py-12 text-gray-500 dark:text-gray-400">
                <SparklesIcon className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>Generated content will appear here</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </Layout>
  )
}
