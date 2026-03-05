import React from 'react'
import Head from 'next/head'
import Layout from '../components/Layout'
import { CogIcon } from '@heroicons/react/24/outline'

export default function SettingsPage() {
  return (
    <Layout>
      <Head>
        <title>Settings - Cloud Native Scanner</title>
        <meta name="description" content="Application settings" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <CogIcon className="h-8 w-8 text-gray-600" />
          <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Application Settings</h2>
          
          <div className="space-y-4">
            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">General</h3>
              <p className="text-sm text-gray-600">General application settings and preferences</p>
            </div>

            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">API Configuration</h3>
              <p className="text-sm text-gray-600">Configure API endpoints and connections</p>
            </div>

            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">Notifications</h3>
              <p className="text-sm text-gray-600">Configure notification preferences</p>
            </div>

            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">Appearance</h3>
              <p className="text-sm text-gray-600">Customize the appearance of the application</p>
            </div>
          </div>

          <div className="mt-6 pt-6 border-t border-gray-200">
            <p className="text-sm text-gray-500">
              Settings functionality coming soon. This page will allow you to configure various application settings.
            </p>
          </div>
        </div>
      </div>
    </Layout>
  )
}
