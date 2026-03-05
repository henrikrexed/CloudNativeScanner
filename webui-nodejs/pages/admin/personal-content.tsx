import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import Layout from '../../components/Layout'
import apiClient from '../../lib/api'
import { PersonalContent } from '../../types'
import { formatDate } from '../../lib/dateUtils'
import { 
  DocumentTextIcon, 
  PlusIcon, 
  PencilIcon, 
  TrashIcon,
  XMarkIcon,
  CheckIcon,
  SparklesIcon,
  TagIcon,
  FolderIcon
} from '@heroicons/react/24/outline'

export default function PersonalContentPage() {
  const [contentList, setContentList] = useState<PersonalContent[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [editingContent, setEditingContent] = useState<PersonalContent | null>(null)
  const [formData, setFormData] = useState({
    title: '',
    content: '',
    contentType: 'GENERAL',
    category: '',
    tagsList: [] as string[]
  })
  const [tagInput, setTagInput] = useState('')
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({})
  const [filters, setFilters] = useState({
    category: '',
    contentType: '',
    tag: ''
  })

  const contentTypeOptions = [
    { value: 'GENERAL', label: 'General' },
    { value: 'BLOG', label: 'Blog Post' },
    { value: 'VIDEO_SCRIPT', label: 'Video Script' },
    { value: 'ARTICLE', label: 'Article' },
    { value: 'DOCUMENTATION', label: 'Documentation' }
  ]

  useEffect(() => {
    fetchContent()
  }, [filters])

  const fetchContent = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await apiClient.getPersonalContent(filters)
      setContentList(data.content || [])
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch personal content'
      setError(errorMessage)
      setContentList([])
    } finally {
      setLoading(false)
    }
  }

  const handleOpenModal = (content?: PersonalContent) => {
    if (content) {
      setEditingContent(content)
      setFormData({
        title: content.title || '',
        content: content.content || '',
        contentType: content.contentType || 'GENERAL',
        category: content.category || '',
        tagsList: content.tagsList || []
      })
    } else {
      setEditingContent(null)
      setFormData({
        title: '',
        content: '',
        contentType: 'GENERAL',
        category: '',
        tagsList: []
      })
    }
    setShowModal(true)
  }

  const handleCloseModal = () => {
    setShowModal(false)
    setEditingContent(null)
    setFormData({
      title: '',
      content: '',
      contentType: 'GENERAL',
      category: '',
      tagsList: []
    })
    setTagInput('')
    setValidationErrors({})
    setError(null)
  }

  const handleAddTag = () => {
    if (tagInput.trim() && !formData.tagsList.includes(tagInput.trim())) {
      setFormData({
        ...formData,
        tagsList: [...formData.tagsList, tagInput.trim()]
      })
      setTagInput('')
    }
  }

  const handleRemoveTag = (tag: string) => {
    setFormData({
      ...formData,
      tagsList: formData.tagsList.filter(t => t !== tag)
    })
  }

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {}

    // Validate title (mandatory)
    if (!formData.title || !formData.title.trim()) {
      errors.title = 'Title is required'
    } else if (formData.title.trim().length > 500) {
      errors.title = 'Title must be 500 characters or less'
    }

    // Validate content (mandatory)
    if (!formData.content || !formData.content.trim()) {
      errors.content = 'Content is required'
    } else if (formData.content.trim().length < 10) {
      errors.content = 'Content must be at least 10 characters long'
    }

    // Validate contentType (mandatory, but has default so should always be set)
    if (!formData.contentType) {
      errors.contentType = 'Content type is required'
    }

    setValidationErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setValidationErrors({})

    // Validate form before submission
    if (!validateForm()) {
      return
    }

    setSubmitting(true)
    
    try {
      const contentData: any = {
        title: formData.title.trim(),
        content: formData.content.trim(),
        contentType: formData.contentType,
        category: formData.category.trim() || null,
        tagsList: formData.tagsList
      }

      if (editingContent) {
        await apiClient.updatePersonalContent(editingContent.id, contentData)
        setSuccess('Personal content updated successfully!')
      } else {
        await apiClient.createPersonalContent(contentData)
        setSuccess('Personal content created successfully!')
      }

      handleCloseModal()
      // Clear filters to show all content including the newly saved one
      setFilters({ category: '', contentType: '', tag: '' })
      // Small delay to ensure filters are cleared before fetching
      setTimeout(async () => {
        await fetchContent()
      }, 100)
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to save content'
      setError(errorMessage)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this content? This action cannot be undone.')) {
      return
    }

    try {
      setDeletingId(id)
      setError(null)
      await apiClient.deletePersonalContent(id)
      setSuccess('Content deleted successfully!')
      await fetchContent()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to delete content'
      setError(errorMessage)
    } finally {
      setDeletingId(null)
    }
  }

  const handleReprocess = async (id: number) => {
    try {
      setError(null)
      await apiClient.reprocessContent(id)
      setSuccess('Content re-processed into RAG successfully!')
      await fetchContent()
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to re-process content'
      setError(errorMessage)
    }
  }

  if (loading && contentList.length === 0) {
    return (
      <Layout>
        <Head>
          <title>Personal Content - Admin - Cloud Native Scanner</title>
        </Head>
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading personal content...</p>
          </div>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <Head>
        <title>Personal Content - Admin - Cloud Native Scanner</title>
        <meta name="description" content="Manage your personal content for RAG system" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <DocumentTextIcon className="h-8 w-8 text-gray-600 dark:text-gray-400" />
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Personal Content</h1>
          </div>
          <button
            onClick={() => handleOpenModal()}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <PlusIcon className="h-5 w-5" />
            Add Content
          </button>
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-200 px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {success && (
          <div className="bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 text-green-800 dark:text-green-200 px-4 py-3 rounded-lg">
            {success}
          </div>
        )}

        {/* Filters */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Category
              </label>
              <input
                type="text"
                value={filters.category}
                onChange={(e) => setFilters({ ...filters, category: e.target.value })}
                placeholder="Filter by category"
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Content Type
              </label>
              <select
                value={filters.contentType}
                onChange={(e) => setFilters({ ...filters, contentType: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
              >
                <option value="">All Types</option>
                {contentTypeOptions.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Tag
              </label>
              <input
                type="text"
                value={filters.tag}
                onChange={(e) => setFilters({ ...filters, tag: e.target.value })}
                placeholder="Filter by tag"
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
              />
            </div>
          </div>
        </div>

        {/* Content List */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          {contentList.length === 0 ? (
            <div className="text-center py-12">
              <DocumentTextIcon className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-600 dark:text-gray-400">No personal content found</p>
              <button
                onClick={() => handleOpenModal()}
                className="mt-4 text-blue-600 hover:text-blue-700"
              >
                Add your first content
              </button>
            </div>
          ) : (
            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {contentList.map((content) => (
                <div key={content.id} className="p-6 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                          {content.title}
                        </h3>
                        <span className="px-2 py-1 text-xs font-medium bg-blue-100 dark:bg-blue-900 text-blue-800 dark:text-blue-200 rounded">
                          {content.contentType}
                        </span>
                        {content.category && (
                          <span className="px-2 py-1 text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded flex items-center gap-1">
                            <FolderIcon className="h-3 w-3" />
                            {content.category}
                          </span>
                        )}
                        {content.ragStored && (
                          <span className="px-2 py-1 text-xs font-medium bg-green-100 dark:bg-green-900 text-green-800 dark:text-green-200 rounded flex items-center gap-1">
                            <CheckIcon className="h-3 w-3" />
                            In RAG
                          </span>
                        )}
                      </div>
                      <p className="text-gray-600 dark:text-gray-400 text-sm mb-3 line-clamp-2">
                        {content.content.substring(0, 200)}...
                      </p>
                      {content.tagsList && content.tagsList.length > 0 && (
                        <div className="flex flex-wrap gap-2 mb-3">
                          {content.tagsList.map((tag, idx) => (
                            <span
                              key={idx}
                              className="px-2 py-1 text-xs bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded flex items-center gap-1"
                            >
                              <TagIcon className="h-3 w-3" />
                              {tag}
                            </span>
                          ))}
                        </div>
                      )}
                      <div className="text-xs text-gray-500 dark:text-gray-400">
                        Created: {formatDate(content.createdAt)}
                        {content.ragStoredAt && ` • RAG stored: ${formatDate(content.ragStoredAt)}`}
                      </div>
                    </div>
                    <div className="flex items-center gap-2 ml-4">
                      <button
                        onClick={() => handleOpenModal(content)}
                        className="p-2 text-gray-600 dark:text-gray-400 hover:text-blue-600 dark:hover:text-blue-400 transition-colors"
                        title="Edit"
                      >
                        <PencilIcon className="h-5 w-5" />
                      </button>
                      {!content.ragStored && (
                        <button
                          onClick={() => handleReprocess(content.id)}
                          className="p-2 text-gray-600 dark:text-gray-400 hover:text-green-600 dark:hover:text-green-400 transition-colors"
                          title="Process into RAG"
                        >
                          <SparklesIcon className="h-5 w-5" />
                        </button>
                      )}
                      <button
                        onClick={() => handleDelete(content.id)}
                        disabled={deletingId === content.id}
                        className="p-2 text-gray-600 dark:text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors disabled:opacity-50"
                        title="Delete"
                      >
                        <TrashIcon className="h-5 w-5" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-center justify-center min-h-screen px-4">
            <div className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm" onClick={handleCloseModal} />
            <div className="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
              <div className="sticky top-0 bg-white dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700 px-6 py-4 flex items-center justify-between z-10">
                <h2 className="text-xl font-bold text-gray-900 dark:text-white">
                  {editingContent ? 'Edit Personal Content' : 'Add Personal Content'}
                </h2>
                <button
                  onClick={handleCloseModal}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
                >
                  <XMarkIcon className="h-6 w-6" />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="p-6 space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Title *
                  </label>
                  <input
                    type="text"
                    value={formData.title}
                    onChange={(e) => {
                      setFormData({ ...formData, title: e.target.value })
                      // Clear validation error when user starts typing
                      if (validationErrors.title) {
                        setValidationErrors({ ...validationErrors, title: '' })
                      }
                    }}
                    required
                    maxLength={500}
                    className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white ${
                      validationErrors.title 
                        ? 'border-red-500 dark:border-red-500' 
                        : 'border-gray-300 dark:border-gray-600'
                    }`}
                  />
                  {validationErrors.title && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{validationErrors.title}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Content *
                  </label>
                  <textarea
                    value={formData.content}
                    onChange={(e) => {
                      setFormData({ ...formData, content: e.target.value })
                      // Clear validation error when user starts typing
                      if (validationErrors.content) {
                        setValidationErrors({ ...validationErrors, content: '' })
                      }
                    }}
                    required
                    rows={10}
                    minLength={10}
                    className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white ${
                      validationErrors.content 
                        ? 'border-red-500 dark:border-red-500' 
                        : 'border-gray-300 dark:border-gray-600'
                    }`}
                    placeholder="Paste your content here..."
                  />
                  {validationErrors.content && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{validationErrors.content}</p>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Content Type *
                    </label>
                    <select
                      value={formData.contentType}
                      onChange={(e) => {
                        setFormData({ ...formData, contentType: e.target.value })
                        // Clear validation error when user changes selection
                        if (validationErrors.contentType) {
                          setValidationErrors({ ...validationErrors, contentType: '' })
                        }
                      }}
                      required
                      className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white ${
                        validationErrors.contentType 
                          ? 'border-red-500 dark:border-red-500' 
                          : 'border-gray-300 dark:border-gray-600'
                      }`}
                    >
                      {contentTypeOptions.map(opt => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                    {validationErrors.contentType && (
                      <p className="mt-1 text-sm text-red-600 dark:text-red-400">{validationErrors.contentType}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      Category
                    </label>
                    <input
                      type="text"
                      value={formData.category}
                      onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                      placeholder="e.g., Tutorial, Documentation"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Tags
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
                  {formData.tagsList.length > 0 && (
                    <div className="flex flex-wrap gap-2">
                      {formData.tagsList.map((tag, idx) => (
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
                            <XMarkIcon className="h-4 w-4" />
                          </button>
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                  <button
                    type="button"
                    onClick={handleCloseModal}
                    className="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  >
                    {submitting ? 'Saving...' : editingContent ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </Layout>
  )
}
