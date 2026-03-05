import React, { useEffect } from 'react'
import { AppProps } from 'next/app'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SWRConfig } from 'swr'
import { Toaster } from 'react-hot-toast'
import { initTracing } from '../lib/tracing'
import { initOpenTelemetry, shutdownOpenTelemetry } from '../lib/otel'
import '../styles/globals.css'

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes
      retry: 3,
      refetchOnWindowFocus: false,
    },
  },
})

// SWR configuration
const swrConfig = {
  revalidateOnFocus: false,
  revalidateOnReconnect: true,
  refreshInterval: 0,
  dedupingInterval: 2000,
  errorRetryCount: 3,
  errorRetryInterval: 5000,
  onError: (error: any) => {
    console.error('SWR Error:', error)
  },
}

export default function App({ Component, pageProps }: AppProps) {
  // Initialize OpenTelemetry tracing on app mount
  useEffect(() => {
    // Initialize proper OpenTelemetry SDK for browser-side tracing
    initOpenTelemetry()
    // Also initialize legacy tracing for backward compatibility
    initTracing()
    
    // Cleanup on unmount
    return () => {
      shutdownOpenTelemetry()
    }
  }, [])

  return (
    <QueryClientProvider client={queryClient}>
      <SWRConfig value={swrConfig}>
        <div className="min-h-screen bg-gray-50">
          <Component {...pageProps} />
          <Toaster
            position="top-right"
            toastOptions={{
              duration: 4000,
              style: {
                background: '#363636',
                color: '#fff',
              },
              success: {
                duration: 3000,
                iconTheme: {
                  primary: '#10B981',
                  secondary: '#fff',
                },
              },
              error: {
                duration: 5000,
                iconTheme: {
                  primary: '#EF4444',
                  secondary: '#fff',
                },
              },
            }}
          />
        </div>
      </SWRConfig>
    </QueryClientProvider>
  )
}
