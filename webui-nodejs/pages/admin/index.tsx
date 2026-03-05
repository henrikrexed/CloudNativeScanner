import React from 'react'
import Head from 'next/head'
import Link from 'next/link'
import Layout from '../../components/Layout'
import { CogIcon, ChartBarIcon, BellIcon, TagIcon } from '@heroicons/react/24/outline'

export default function AdminPage() {
  return (
    <Layout>
      <Head>
        <title>Admin - Cloud Native Scanner</title>
        <meta name="description" content="Administration panel" />
      </Head>

      <div className="space-y-6">
        <h1 className="text-3xl font-bold text-gray-900">Administration</h1>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          <Link href="/admin/sources">
            <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow cursor-pointer">
              <CogIcon className="h-8 w-8 text-blue-600 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Manage Sources</h3>
              <p className="text-gray-600 text-sm">Configure and manage data sources</p>
            </div>
          </Link>

          <Link href="/admin/monitoring">
            <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow cursor-pointer">
              <BellIcon className="h-8 w-8 text-green-600 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Monitoring</h3>
              <p className="text-gray-600 text-sm">View system health and metrics</p>
            </div>
          </Link>

          <Link href="/admin/themes">
            <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow cursor-pointer">
              <TagIcon className="h-8 w-8 text-orange-600 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Manage Themes</h3>
              <p className="text-gray-600 text-sm">Add, edit, and delete themes and categories</p>
            </div>
          </Link>

          <Link href="/admin/settings">
            <div className="bg-white rounded-lg shadow p-6 hover:shadow-lg transition-shadow cursor-pointer">
              <ChartBarIcon className="h-8 w-8 text-purple-600 mb-4" />
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Settings</h3>
              <p className="text-gray-600 text-sm">Configure application settings</p>
            </div>
          </Link>
        </div>
      </div>
    </Layout>
  )
}

