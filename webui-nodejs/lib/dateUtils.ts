/**
 * Utility functions for safe date formatting
 */

export function formatDate(dateString: string | null | undefined, options?: Intl.DateTimeFormatOptions): string {
  if (!dateString) return 'N/A'
  
  try {
    const date = new Date(dateString)
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return 'Invalid date'
    }
    
    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }
    
    return date.toLocaleDateString('en-US', options || defaultOptions)
  } catch (error) {
    console.error('Error formatting date:', dateString, error)
    return 'Invalid date'
  }
}

export function formatDateTime(dateString: string | null | undefined, options?: Intl.DateTimeFormatOptions): string {
  if (!dateString) return 'N/A'
  
  try {
    const date = new Date(dateString)
    // Check if date is valid
    if (isNaN(date.getTime())) {
      return 'Invalid date'
    }
    
    const defaultOptions: Intl.DateTimeFormatOptions = {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }
    
    return date.toLocaleString('en-US', options || defaultOptions)
  } catch (error) {
    console.error('Error formatting date/time:', dateString, error)
    return 'Invalid date'
  }
}

export function formatDateLong(dateString: string | null | undefined): string {
  return formatDate(dateString, {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

