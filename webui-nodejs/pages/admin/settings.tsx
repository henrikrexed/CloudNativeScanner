import React from 'react'
import Head from 'next/head'
import Layout from '../../components/Layout'
import { CogIcon } from '@heroicons/react/24/outline'

export default function AdminSettingsPage() {
  return (
    <Layout>
      <Head>
        <title>Admin Settings - Cloud Native Scanner</title>
        <meta name="description" content="Administration settings" />
      </Head>

      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <CogIcon className="h-8 w-8 text-gray-600" />
          <h1 className="text-3xl font-bold text-gray-900">Admin Settings</h1>
        </div>

        <div className="bg-white rounded-lg shadow p-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Administration Settings</h2>
          
          <div className="space-y-4">
            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">System Configuration</h3>
              <p className="text-sm text-gray-600">Configure system-wide settings and preferences</p>
            </div>

            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">Database Configuration</h3>
              <p className="text-sm text-gray-600">Manage database connections and settings</p>
            </div>

            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">Kafka Configuration</h3>
              <p className="text-sm text-gray-600">Configure Kafka broker settings and topics</p>
            </div>

            <div className="border-b border-gray-200 pb-4">
              <h3 className="text-lg font-medium text-gray-900 mb-2">AI Provider Settings</h3>
              <p className="text-sm text-gray-600">Configure AI provider (OpenAI, Anthropic, etc.) settings</p>
            </div>

            <div>
              <h3 className="text-lg font-medium text-gray-900 mb-2">Security Settings</h3>
              <p className="text-sm text-gray-600">Manage security and authentication settings</p>
            </div>
          </div>

          <div className="mt-6 pt-6 border-t border-gray-200">
            <p className="text-sm text-gray-500">
              Admin settings functionality coming soon. This page will allow you to configure system-wide administration settings.
            </p>
          </div>
        </div>
      </div>
    </Layout>
  )
}
