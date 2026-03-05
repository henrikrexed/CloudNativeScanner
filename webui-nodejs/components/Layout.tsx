import React, { useState, useEffect } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/router'
import { motion, AnimatePresence } from 'framer-motion'
import { 
  HomeIcon,
  ChartBarIcon,
  CogIcon,
  BellIcon,
  DocumentTextIcon,
  MagnifyingGlassIcon,
  Bars3Icon,
  XMarkIcon,
  SunIcon,
  MoonIcon,
  UserCircleIcon,
  SparklesIcon,
  PencilSquareIcon
} from '@heroicons/react/24/outline'
import { 
  HomeIcon as HomeIconSolid,
  ChartBarIcon as ChartBarIconSolid,
  CogIcon as CogIconSolid,
  DocumentTextIcon as DocumentTextIconSolid,
  MagnifyingGlassIcon as MagnifyingGlassIconSolid
} from '@heroicons/react/24/solid'

interface LayoutProps {
  children: React.ReactNode
}

const navigation = [
  { name: 'Dashboard', href: '/', icon: HomeIcon, iconSolid: HomeIconSolid },
  { name: 'Topics', href: '/topics', icon: DocumentTextIcon, iconSolid: DocumentTextIconSolid },
  { name: 'Themes', href: '/themes', icon: ChartBarIcon, iconSolid: ChartBarIconSolid },
  { name: 'Search', href: '/search', icon: MagnifyingGlassIcon, iconSolid: MagnifyingGlassIconSolid },
]

const adminNavigation = [
  { name: 'Admin', href: '/admin', icon: CogIcon, iconSolid: CogIconSolid },
  { name: 'Monitoring', href: '/admin/monitoring', icon: BellIcon },
]

const contentCreationNavigation = [
  { name: 'Personal Content', href: '/admin/personal-content', icon: PencilSquareIcon, iconSolid: PencilSquareIcon },
  { name: 'Content Generator', href: '/admin/content-generator', icon: SparklesIcon, iconSolid: SparklesIcon },
]

export default function Layout({ children }: LayoutProps) {
  const router = useRouter()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [darkMode, setDarkMode] = useState(false)

  useEffect(() => {
    // Check for saved theme preference or default to light mode
    const isDark = localStorage.getItem('theme') === 'dark' || 
                   (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches)
    setDarkMode(isDark)
    if (isDark) {
      document.documentElement.classList.add('dark')
    }
  }, [])

  const toggleDarkMode = () => {
    const newDarkMode = !darkMode
    setDarkMode(newDarkMode)
    localStorage.setItem('theme', newDarkMode ? 'dark' : 'light')
    if (newDarkMode) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  const isActive = (href: string) => {
    if (href === '/') {
      return router.pathname === '/'
    }
    return router.pathname.startsWith(href)
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 transition-colors duration-200">
      {/* Mobile sidebar overlay */}
      <AnimatePresence>
        {sidebarOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="fixed inset-0 z-40 bg-gray-900/50 backdrop-blur-sm lg:hidden"
              onClick={() => setSidebarOpen(false)}
            />
            <motion.div
              initial={{ x: -320 }}
              animate={{ x: 0 }}
              exit={{ x: -320 }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="fixed inset-y-0 left-0 z-50 w-80 bg-white dark:bg-gray-800 shadow-2xl lg:hidden"
            >
              <div className="flex h-full flex-col">
                {/* Mobile sidebar header */}
                <div className="flex h-16 items-center justify-between border-b border-gray-200 dark:border-gray-700 px-4">
                  <h1 className="text-xl font-bold bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent">
                    Cloud Native Scanner
                  </h1>
                  <button
                    onClick={() => setSidebarOpen(false)}
                    className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                  >
                    <XMarkIcon className="h-6 w-6" />
                  </button>
                </div>

                {/* Mobile sidebar navigation */}
                <nav className="flex-1 space-y-1 px-3 py-4 overflow-y-auto">
                  {navigation.map((item) => {
                    const active = isActive(item.href)
                    const Icon = active ? item.iconSolid : item.icon
                    return (
                      <Link
                        key={item.name}
                        href={item.href}
                        onClick={() => setSidebarOpen(false)}
                        className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                          active
                            ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/30'
                            : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                        }`}
                      >
                        <Icon className={`h-5 w-5 flex-shrink-0 ${active ? 'text-white' : 'text-gray-500 dark:text-gray-400'}`} />
                        {item.name}
                      </Link>
                    )
                  })}
                  
                  {/* Content Creation section */}
                  <div className="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
                    <p className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                      Content Creation
                    </p>
                    {contentCreationNavigation.map((item) => {
                      const active = isActive(item.href)
                      const Icon = (active && item.iconSolid) ? item.iconSolid : item.icon
                      return (
                        <Link
                          key={item.name}
                          href={item.href}
                          onClick={() => setSidebarOpen(false)}
                          className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                            active
                              ? 'bg-gray-100 dark:bg-gray-700 text-primary-600 dark:text-primary-400'
                              : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                          }`}
                        >
                          <Icon className="h-5 w-5 flex-shrink-0 text-gray-500 dark:text-gray-400" />
                          {item.name}
                        </Link>
                      )
                    })}
                  </div>

                  {/* Admin section */}
                  <div className="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
                    <p className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                      Administration
                    </p>
                    {adminNavigation.map((item) => {
                      const active = isActive(item.href)
                      const Icon = active && item.iconSolid ? item.iconSolid : item.icon
                      return (
                        <Link
                          key={item.name}
                          href={item.href}
                          onClick={() => setSidebarOpen(false)}
                          className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                            active
                              ? 'bg-gray-100 dark:bg-gray-700 text-primary-600 dark:text-primary-400'
                              : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                          }`}
                        >
                          <Icon className="h-5 w-5 flex-shrink-0 text-gray-500 dark:text-gray-400" />
                          {item.name}
                        </Link>
                      )
                    })}
                  </div>
                </nav>
              </div>
            </motion.div>
          </>
        )}
      </AnimatePresence>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-72 lg:flex-col">
        <div className="flex min-h-0 flex-1 flex-col bg-white dark:bg-gray-800 border-r border-gray-200 dark:border-gray-700">
          {/* Logo/Header */}
          <div className="flex h-16 flex-shrink-0 items-center justify-between border-b border-gray-200 dark:border-gray-700 px-6">
            <h1 className="text-xl font-bold bg-gradient-to-r from-primary-600 to-accent-600 bg-clip-text text-transparent">
              Cloud Native Scanner
            </h1>
          </div>

          {/* Navigation */}
          <nav className="flex flex-1 flex-col gap-1 px-3 py-4 overflow-y-auto scrollbar-hide">
            {navigation.map((item) => {
              const active = isActive(item.href)
              const Icon = active ? item.iconSolid : item.icon
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  className={`group relative flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                    active
                      ? 'bg-gradient-to-r from-primary-500 to-primary-600 text-white shadow-lg shadow-primary-500/30'
                      : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                  }`}
                >
                  <Icon className={`h-5 w-5 flex-shrink-0 transition-transform ${active ? 'text-white scale-110' : 'text-gray-500 dark:text-gray-400 group-hover:scale-105'}`} />
                  <span className="flex-1">{item.name}</span>
                  {active && (
                    <motion.div
                      layoutId="activeIndicator"
                      className="absolute right-2 w-1.5 h-1.5 rounded-full bg-white"
                      initial={false}
                      transition={{ type: 'spring', stiffness: 500, damping: 30 }}
                    />
                  )}
                </Link>
              )
            })}

            {/* Content Creation section */}
            <div className="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
              <p className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                Content Creation
              </p>
              {contentCreationNavigation.map((item) => {
                const active = isActive(item.href)
                const Icon = (active && item.iconSolid) ? item.iconSolid : item.icon
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                      active
                        ? 'bg-gray-100 dark:bg-gray-700 text-primary-600 dark:text-primary-400'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                    }`}
                  >
                    <Icon className="h-5 w-5 flex-shrink-0 text-gray-500 dark:text-gray-400" />
                    <span>{item.name}</span>
                  </Link>
                )
              })}
            </div>

            {/* Admin section */}
            <div className="pt-4 mt-4 border-t border-gray-200 dark:border-gray-700">
              <p className="px-3 text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-2">
                Administration
              </p>
              {adminNavigation.map((item) => {
                const active = isActive(item.href)
                const Icon = active && item.iconSolid ? item.iconSolid : item.icon
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-200 ${
                      active
                        ? 'bg-gray-100 dark:bg-gray-700 text-primary-600 dark:text-primary-400'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700/50'
                    }`}
                  >
                    <Icon className="h-5 w-5 flex-shrink-0 text-gray-500 dark:text-gray-400" />
                    <span>{item.name}</span>
                  </Link>
                )
              })}
            </div>
          </nav>
        </div>
      </div>

      {/* Main content area */}
      <div className="lg:pl-72">
        {/* Top navigation bar */}
        <div className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-x-4 border-b border-gray-200 dark:border-gray-700 bg-white/80 dark:bg-gray-800/80 backdrop-blur-lg px-4 shadow-sm sm:gap-x-6 sm:px-6 lg:px-8">
          {/* Mobile menu button */}
          <button
            type="button"
            className="-m-2.5 p-2.5 text-gray-700 dark:text-gray-300 lg:hidden"
            onClick={() => setSidebarOpen(true)}
          >
            <Bars3Icon className="h-6 w-6" />
          </button>

          {/* Search bar - placeholder for future */}
          <div className="flex flex-1 gap-x-4 lg:gap-x-6">
            <div className="hidden sm:flex flex-1 max-w-md">
              {/* Search will be added here */}
            </div>
          </div>

          {/* Right side actions */}
          <div className="flex items-center gap-x-3 lg:gap-x-4">
            {/* Theme toggle */}
            <button
              onClick={toggleDarkMode}
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400 transition-colors"
              aria-label="Toggle dark mode"
            >
              {darkMode ? (
                <SunIcon className="h-5 w-5" />
              ) : (
                <MoonIcon className="h-5 w-5" />
              )}
            </button>

            {/* Notifications */}
            <button
              className="relative rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400 transition-colors"
              aria-label="Notifications"
            >
              <BellIcon className="h-5 w-5" />
              <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-accent-500"></span>
            </button>

            {/* User menu */}
            <button
              className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400 transition-colors"
              aria-label="User menu"
            >
              <UserCircleIcon className="h-6 w-6" />
            </button>
          </div>
        </div>

        {/* Page content */}
        <main className="py-6 sm:py-8">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {children}
          </div>
        </main>
      </div>
    </div>
  )
}
