import React, { useState, useEffect } from 'react'
import Head from 'next/head'
import Layout from '../../components/Layout'
import apiClient from '../../lib/api'
import { Theme } from '../../types'
import { formatDate } from '../../lib/dateUtils'
import { 
  TagIcon, 
  PlusIcon, 
  PencilIcon, 
  TrashIcon,
  XMarkIcon,
  CheckIcon,
  ArrowPathIcon,
  SparklesIcon
} from '@heroicons/react/24/outline'
import { Switch } from '@headlessui/react'

export default function AdminThemesPage() {
  const [themes, setThemes] = useState<Theme[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [showModal, setShowModal] = useState(false)
  const [editingTheme, setEditingTheme] = useState<Theme | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    parentThemeId: '' as string | number | undefined,
    keywords: [] as string[]
  })
  const [regeneratingKeywords, setRegeneratingKeywords] = useState<number | null>(null)
  const [topLevelThemes, setTopLevelThemes] = useState<Theme[]>([])
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [togglingId, setTogglingId] = useState<number | null>(null)

  useEffect(() => {
    fetchThemes()
    fetchTopLevelThemes()
  }, [])

  const fetchThemes = async () => {
    try {
      setLoading(true)
      setError(null)
      console.log('Fetching themes from API (including disabled)...')
      // Fetch all themes including disabled ones for admin management
      const data = await apiClient.getThemes(true)
      console.log('Themes fetched successfully:', data.length, 'themes')
      // Log enabled status for debugging
      data.forEach(theme => {
        if (theme.id === 7) {
          console.log('Theme 7 enabled status:', theme.enabled, 'type:', typeof theme.enabled)
        }
      })
      setThemes(data || [])
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to fetch themes'
      setError(errorMessage)
      console.error('Error fetching themes:', err)
      console.error('Error response:', err.response)
      // Set empty array on error to prevent UI issues
      setThemes([])
    } finally {
      setLoading(false)
    }
  }

  const fetchTopLevelThemes = async () => {
    try {
      const data = await apiClient.getTopLevelThemes()
      setTopLevelThemes(data || [])
    } catch (err) {
      console.error('Error fetching top-level themes:', err)
      setTopLevelThemes([])
    }
  }

  const handleOpenModal = (theme?: Theme) => {
    console.log('Opening modal for theme:', theme)
    if (theme) {
      setEditingTheme(theme)
      // Parse keywords from theme (can be JSON array or comma-separated string)
      // Backend stores keywords in searchKeywords field
      let keywords: string[] = []
      if (theme.keywords && Array.isArray(theme.keywords)) {
        keywords = theme.keywords
      } else if (theme.searchKeywords) {
        try {
          // Try parsing as JSON first
          const parsed = JSON.parse(theme.searchKeywords)
          keywords = Array.isArray(parsed) ? parsed : []
        } catch {
          // If not JSON, split by comma
          keywords = theme.searchKeywords.split(',').map(k => k.trim()).filter(k => k.length > 0)
        }
      }
      // Ensure keywords is always an array (even if empty)
      if (!Array.isArray(keywords)) {
        keywords = []
      }
      
      setFormData({
        name: theme.name || '',
        description: theme.description || '',
        parentThemeId: theme.parentTheme?.id || '',
        keywords: keywords
      })
    } else {
      setEditingTheme(null)
      setFormData({
        name: '',
        description: '',
        parentThemeId: '',
        keywords: []
      })
    }
    setShowModal(true)
    console.log('Modal should be visible now, showModal:', true)
  }

  const handleCloseModal = () => {
    setShowModal(false)
    setEditingTheme(null)
    setFormData({
      name: '',
      description: '',
      parentThemeId: '',
      keywords: []
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    console.log('Form submitted:', { editingTheme, formData })
    setSubmitting(true)
    setError(null)
    setSuccess(null)
    
    try {
      const themeData: any = {
        name: formData.name.trim(),
        description: formData.description.trim() || null
      }

      // Include keywords if they exist in formData
      if (formData.keywords && Array.isArray(formData.keywords) && formData.keywords.length > 0) {
        // Send as searchKeywords (JSON string) - backend will parse it
        themeData.searchKeywords = JSON.stringify(formData.keywords)
      }

      // If parent theme is selected, include it in the request
      if (formData.parentThemeId && formData.parentThemeId !== '') {
        const parentTheme = topLevelThemes.find(t => t.id === Number(formData.parentThemeId))
        if (parentTheme) {
          // Send minimal parent theme object with just the ID
          themeData.parentTheme = { id: parentTheme.id }
        }
      } else if (editingTheme && editingTheme.parentTheme) {
        // If editing and removing parent, explicitly set to null
        themeData.parentTheme = null
      }

      console.log('Sending theme data:', themeData)

      if (editingTheme) {
        console.log('Updating theme:', editingTheme.id)
        await apiClient.updateTheme(editingTheme.id, themeData)
        console.log('Theme updated successfully')
        setSuccess('Theme updated successfully!')
      } else {
        console.log('Creating new theme')
        await apiClient.createTheme(themeData)
        console.log('Theme created successfully')
        setSuccess('Theme created successfully!')
      }

      handleCloseModal()
      await fetchThemes()
      await fetchTopLevelThemes()
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to save theme'
      setError(errorMessage)
      console.error('Error saving theme:', err)
      console.error('Error details:', err.response?.data)
      console.error('Error status:', err.response?.status)
      console.error('Error config:', err.config)
    } finally {
      setSubmitting(false)
    }
  }

  const handleRegenerateKeywords = async (themeId: number) => {
    try {
      setRegeneratingKeywords(themeId)
      setError(null)
      setSuccess(null)
      
      // Ensure we have a valid theme
      const theme = themes.find(t => t.id === themeId)
      if (!theme) {
        throw new Error('Theme not found')
      }
      console.log('Regenerating keywords for theme:', themeId)
      const result = await apiClient.regenerateThemeKeywords(themeId)
      console.log('Keywords regenerated successfully:', result.keywords)
      setSuccess(`Keywords regenerated successfully! Generated ${result.keywords.length} keywords.`)
      
      // If editing this theme, update the form with new keywords
      if (editingTheme && editingTheme.id === themeId) {
        setFormData({ ...formData, keywords: result.keywords })
      }
      
      await fetchThemes()
      await fetchTopLevelThemes()
      setTimeout(() => setSuccess(null), 5000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.error || err.message || 'Failed to regenerate keywords'
      setError(errorMessage)
      console.error('Error regenerating keywords:', err)
    } finally {
      setRegeneratingKeywords(null)
    }
  }

  const handleToggleEnabled = async (theme: Theme) => {
    // Default to true if enabled is undefined/null (for backward compatibility)
    const currentEnabled = theme.enabled !== undefined && theme.enabled !== null ? theme.enabled : true
    const newEnabled = !currentEnabled
    try {
      setTogglingId(theme.id)
      setError(null)
      setSuccess(null)
      console.log('Toggling enabled state for theme:', theme.id, 'from', currentEnabled, 'to', newEnabled)
      
      // Use dedicated toggle endpoint for better reliability
      const updatedTheme = await apiClient.toggleThemeEnabled(theme.id, newEnabled)
      console.log('Theme enabled state updated successfully. New state:', updatedTheme.enabled)
      
      // Update local state immediately for better UX
      // Create a new object to ensure React detects the change
      setThemes(prevThemes => 
        prevThemes.map(t => {
          if (t.id === theme.id) {
            const updated = { ...t, enabled: updatedTheme.enabled }
            console.log('Updating theme in state:', t.id, 'from', t.enabled, 'to', updated.enabled)
            return updated
          }
          return t
        })
      )
      
      setSuccess(`Theme ${newEnabled ? 'enabled' : 'disabled'} successfully!`)
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000)
      
      // Don't refresh immediately - the local state update is sufficient
      // The state will be refreshed on next page load or manual refresh
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to update theme'
      setError(errorMessage)
      console.error('Error updating theme:', err)
      console.error('Error details:', err.response?.data)
      console.error('Error status:', err.response?.status)
      
      // Revert on error - refresh themes
      await fetchThemes()
    } finally {
      setTogglingId(null)
    }
  }

  const handleDelete = async (id: number) => {
    console.log('Delete button clicked for theme ID:', id)
    if (!window.confirm('Are you sure you want to delete this theme? This action cannot be undone.')) {
      console.log('Delete cancelled by user')
      return
    }

    try {
      setDeletingId(id)
      setError(null)
      setSuccess(null)
      console.log('Calling API to delete theme:', id)
      await apiClient.deleteTheme(id)
      console.log('Theme deleted successfully')
      setSuccess('Theme deleted successfully!')
      await fetchThemes()
      await fetchTopLevelThemes()
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      const errorMessage = err.response?.data?.message || err.message || 'Failed to delete theme'
      setError(errorMessage)
      console.error('Error deleting theme:', err)
      console.error('Error details:', err.response?.data)
      console.error('Error status:', err.response?.status)
    } finally {
      setDeletingId(null)
    }
  }

  const getFullPath = (theme: Theme): string => {
    if (theme.fullPath) {
      return theme.fullPath
    }
    if (theme.parentTheme) {
      return `${theme.parentTheme.name}/${theme.name}`
    }
    return theme.name
  }

  const groupedThemes = themes.reduce((acc, theme) => {
    const parentId = theme.parentTheme?.id || 'top-level'
    if (!acc[parentId]) {
      acc[parentId] = []
    }
    acc[parentId].push(theme)
    return acc
  }, {} as Record<string | number, Theme[]>)

  if (loading && themes.length === 0) {
    return (
      <Layout>
        <Head>
          <title>Manage Themes - Admin - Cloud Native Scanner</title>
        </Head>
        <div className="flex items-center justify-center min-h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading themes...</p>
          </div>
        </div>
      </Layout>
    )
  }

  return (
    <Layout>
      <Head>
        <title>Manage Themes - Admin - Cloud Native Scanner</title>
        <meta name="description" content="Manage themes and categories" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <TagIcon className="h-8 w-8 text-gray-600 dark:text-gray-400" />
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Manage Themes</h1>
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={() => {
                console.log('Refresh button clicked')
                fetchThemes()
                fetchTopLevelThemes()
              }}
              disabled={loading}
              className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors disabled:opacity-50"
              title="Refresh themes list"
            >
              <ArrowPathIcon className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
            <button
              onClick={() => {
                console.log('Add Theme button clicked')
                handleOpenModal()
              }}
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <PlusIcon className="h-5 w-5" />
              Add Theme
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg flex items-center justify-between">
            <span>{error}</span>
            <button
              onClick={() => setError(null)}
              className="text-red-700 hover:text-red-900"
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          </div>
        )}

        {success && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg flex items-center justify-between">
            <span>{success}</span>
            <button
              onClick={() => setSuccess(null)}
              className="text-green-700 hover:text-green-900"
            >
              <XMarkIcon className="h-5 w-5" />
            </button>
          </div>
        )}

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
          <div className="p-6">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-gray-900 dark:text-white">
                Themes ({themes.length})
              </h2>
            </div>

            {themes.length === 0 ? (
              <div className="text-center py-12">
                <TagIcon className="h-12 w-12 text-gray-400 dark:text-gray-500 mx-auto mb-4" />
                <p className="text-gray-600 dark:text-gray-400">No themes found. Create your first theme to get started.</p>
              </div>
            ) : (
              <div className="space-y-6">
                {/* Top-level themes */}
                {groupedThemes['top-level'] && groupedThemes['top-level'].length > 0 && (
                  <div>
                    <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-3">Top-Level Themes</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                      {groupedThemes['top-level'].map((theme) => (
                        <ThemeCard
                          key={theme.id}
                          theme={theme}
                          onEdit={() => handleOpenModal(theme)}
                          onDelete={() => handleDelete(theme.id)}
                          onToggleEnabled={() => handleToggleEnabled(theme)}
                          isDeleting={deletingId === theme.id}
                          isToggling={togglingId === theme.id}
                          getFullPath={getFullPath}
                        />
                      ))}
                    </div>
                  </div>
                )}

                {/* Sub-themes grouped by parent */}
                {Object.entries(groupedThemes)
                  .filter(([key]) => key !== 'top-level')
                  .map(([parentId, subThemes]) => {
                    const parentTheme = themes.find(t => t.id === Number(parentId))
                    return (
                      <div key={parentId}>
                        <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-3">
                          {parentTheme ? `${parentTheme.name} - Sub-themes` : `Sub-themes (Parent ID: ${parentId})`}
                        </h3>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                          {subThemes.map((theme) => (
                            <ThemeCard
                              key={theme.id}
                              theme={theme}
                              onEdit={() => handleOpenModal(theme)}
                              onDelete={() => handleDelete(theme.id)}
                              onToggleEnabled={() => handleToggleEnabled(theme)}
                              isDeleting={deletingId === theme.id}
                              isToggling={togglingId === theme.id}
                              getFullPath={getFullPath}
                            />
                          ))}
                        </div>
                      </div>
                    )
                  })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Modal for Add/Edit */}
      {showModal && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              handleCloseModal()
            }
          }}
        >
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white">
                  {editingTheme ? 'Edit Theme' : 'Add New Theme'}
                </h2>
                <button
                  onClick={handleCloseModal}
                  className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
                  disabled={submitting}
                >
                  <XMarkIcon className="h-6 w-6" />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Theme Name *
                  </label>
                  <input
                    type="text"
                    id="name"
                    required
                    disabled={submitting}
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white disabled:opacity-50"
                    placeholder="e.g., Kubernetes, Networking, Security"
                  />
                </div>

                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Description
                  </label>
                  <textarea
                    id="description"
                    rows={4}
                    disabled={submitting}
                    value={formData.description}
                    onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white disabled:opacity-50"
                    placeholder="Describe what this theme covers..."
                  />
                </div>

                <div>
                  <label htmlFor="parentTheme" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    Parent Theme (Optional)
                  </label>
                  <select
                    id="parentTheme"
                    disabled={submitting}
                    value={formData.parentThemeId || ''}
                    onChange={(e) => setFormData({ ...formData, parentThemeId: e.target.value || '' })}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white disabled:opacity-50"
                  >
                    <option value="">None (Top-level theme)</option>
                    {topLevelThemes.map((theme) => (
                      <option key={theme.id} value={theme.id}>
                        {theme.name}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    Select a parent theme to create a hierarchical sub-category (e.g., "Kubernetes/Networking")
                  </p>
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label htmlFor="keywords" className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                      Search Keywords
                    </label>
                    {editingTheme && (
                      <button
                        type="button"
                        onClick={() => handleRegenerateKeywords(editingTheme.id)}
                        disabled={submitting || regeneratingKeywords === editingTheme.id}
                        className="inline-flex items-center gap-1 text-xs px-2 py-1 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors disabled:opacity-50"
                        title="Regenerate keywords using AI"
                      >
                        {regeneratingKeywords === editingTheme.id ? (
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
                    )}
                  </div>
                  <textarea
                    id="keywords"
                    rows={4}
                    disabled={submitting}
                    value={formData.keywords.join(', ')}
                    onChange={(e) => {
                      const keywords = e.target.value.split(',').map(k => k.trim()).filter(k => k)
                      setFormData({ ...formData, keywords })
                    }}
                    className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 dark:bg-gray-700 dark:text-white disabled:opacity-50 font-mono text-sm"
                    placeholder="Enter search keywords separated by commas (e.g., kubernetes, k8s, container orchestration, kubectl, pods)"
                  />
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    Search keywords used by scanners to find content for this theme. These are short phrases (1-3 words) that help find relevant discussions, tutorials, and articles on platforms like Reddit, StackOverflow, YouTube, etc. Separate multiple keywords with commas. 
                    {editingTheme && ' Click "Regenerate with AI" to generate 10 keywords automatically.'}
                  </p>
                  {formData.keywords.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {formData.keywords.map((keyword, index) => (
                        <span
                          key={index}
                          className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200 rounded text-xs"
                        >
                          {keyword}
                          <button
                            type="button"
                            onClick={() => {
                              const newKeywords = formData.keywords.filter((_, i) => i !== index)
                              setFormData({ ...formData, keywords: newKeywords })
                            }}
                            className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200"
                          >
                            <XMarkIcon className="h-3 w-3" />
                          </button>
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="flex items-center justify-end gap-3 pt-4 border-t dark:border-gray-700">
                  <button
                    type="button"
                    onClick={handleCloseModal}
                    disabled={submitting}
                    className="px-4 py-2 text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition-colors disabled:opacity-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {submitting ? (
                      <>
                        <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                        {editingTheme ? 'Updating...' : 'Creating...'}
                      </>
                    ) : (
                      <>
                        <CheckIcon className="h-5 w-5" />
                        {editingTheme ? 'Update Theme' : 'Create Theme'}
                      </>
                    )}
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

interface ThemeCardProps {
  theme: Theme
  onEdit: () => void
  onDelete: () => void
  onToggleEnabled: () => void
  isDeleting: boolean
  isToggling: boolean
  getFullPath: (theme: Theme) => string
}

function ThemeCard({ theme, onEdit, onDelete, onToggleEnabled, isDeleting, isToggling, getFullPath }: ThemeCardProps) {
  // Parse keywords from theme (backend stores keywords in searchKeywords field)
  let keywords: string[] = []
  if (theme.keywords && Array.isArray(theme.keywords)) {
    keywords = theme.keywords
  } else if (theme.searchKeywords) {
    try {
      const parsed = JSON.parse(theme.searchKeywords)
      keywords = Array.isArray(parsed) ? parsed : []
    } catch {
      // Fallback: treat as comma-separated
      keywords = theme.searchKeywords.split(',').map(k => k.trim()).filter(k => k.length > 0)
    }
  }
  // Ensure keywords is always an array
  if (!Array.isArray(keywords)) {
    keywords = []
  }

  // Default to true if enabled is undefined/null (for backward compatibility)
  const isEnabled = theme.enabled !== undefined && theme.enabled !== null ? theme.enabled : true

  return (
    <div className={`border rounded-lg p-4 hover:shadow-md transition-shadow bg-white dark:bg-gray-800 ${
      isEnabled 
        ? 'border-gray-200 dark:border-gray-700' 
        : 'border-gray-300 dark:border-gray-600 opacity-75'
    }`}>
      <div className="flex items-start justify-between mb-2">
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h4 className="font-semibold text-gray-900 dark:text-white">{getFullPath(theme)}</h4>
            {!isEnabled && (
              <span className="px-2 py-0.5 text-xs font-medium bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded">
                Disabled
              </span>
            )}
          </div>
          {theme.parentTheme && (
            <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
              Sub-theme of: {theme.parentTheme.name}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onEdit}
            disabled={isDeleting || isToggling}
            className="p-1.5 text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded transition-colors disabled:opacity-50"
            title="Edit theme"
          >
            <PencilIcon className="h-4 w-4" />
          </button>
          <button
            onClick={onDelete}
            disabled={isDeleting || isToggling}
            className="p-1.5 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition-colors disabled:opacity-50"
            title="Delete theme"
          >
            {isDeleting ? (
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-red-600"></div>
            ) : (
              <TrashIcon className="h-4 w-4" />
            )}
          </button>
        </div>
      </div>
      
      {/* Enable/Disable Toggle */}
      <div className="flex items-center justify-between mb-2 pt-2 border-t border-gray-200 dark:border-gray-700">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Enable for scanning
          </span>
          {isToggling && (
            <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
          )}
        </div>
        <Switch
          checked={isEnabled}
          onChange={() => {
            // Call the toggle handler - it will calculate the new state
            onToggleEnabled()
          }}
          disabled={isToggling || isDeleting}
          className={`${
            isEnabled ? 'bg-blue-600' : 'bg-gray-200 dark:bg-gray-700'
          } relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed`}
        >
          <span
            className={`${
              isEnabled ? 'translate-x-6' : 'translate-x-1'
            } inline-block h-4 w-4 transform rounded-full bg-white transition-transform`}
          />
        </Switch>
      </div>
      {theme.description && (
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-2 line-clamp-2">{theme.description}</p>
      )}
      {keywords.length > 0 && (
        <div className="mb-2">
          <p className="text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Search Keywords:</p>
          <div className="flex flex-wrap gap-1">
            {keywords.slice(0, 5).map((keyword, index) => (
              <span
                key={index}
                className="inline-block px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded text-xs"
              >
                {keyword}
              </span>
            ))}
            {keywords.length > 5 && (
              <span className="inline-block px-2 py-0.5 text-gray-500 dark:text-gray-400 text-xs">
                +{keywords.length - 5} more
              </span>
            )}
          </div>
        </div>
      )}
      <div className="flex items-center gap-4 text-xs text-gray-500 dark:text-gray-400">
        <span>Topics: {theme.topicCount || 0}</span>
        {theme.createdAt && (
          <span>Created: {formatDate(theme.createdAt)}</span>
        )}
      </div>
    </div>
  )
}
