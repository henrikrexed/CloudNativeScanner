import { test, expect } from '@playwright/test'

test.describe('Page Functionality', () => {
  test('home page should load and display content', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check page title
    await expect(page).toHaveTitle(/Dashboard|Cloud Native Scanner/)
    
    // Check main heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Welcome|Dashboard/i)
    
    // Check that stat cards are present (even if empty)
    const statsSection = page.locator('text=/Total Topics|Active Sources|New Today|Themes/i')
    await expect(statsSection.first()).toBeVisible({ timeout: 5000 })
    
    // Check for quick action cards
    const quickActions = page.locator('text=/Search Topics|Browse Topics|View Themes/i')
    await expect(quickActions.first()).toBeVisible({ timeout: 5000 })
    
    // Verify no console errors
    const errors: string[] = []
    page.on('console', msg => {
      if (msg.type() === 'error') {
        errors.push(msg.text())
      }
    })
    
    await page.waitForTimeout(2000) // Wait for any async operations
    
    // Filter out known non-critical errors (like API failures)
    const criticalErrors = errors.filter(e => 
      !e.includes('Failed to fetch') && 
      !e.includes('NetworkError') &&
      !e.includes('404')
    )
    expect(criticalErrors.length).toBe(0)
  })

  test('topics page should load with modern card layout', async ({ page }) => {
    await page.goto('/topics')
    await page.waitForLoadState('networkidle')
    
    // Check page loads without 404
    await expect(page).not.toHaveURL(/404/)
    
    // Check page heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Topics/i)
    
    // Check for search bar
    const searchInput = page.locator('input[type="text"]').first()
    await expect(searchInput).toBeVisible({ timeout: 5000 })
    
    // Check for content area (cards or empty state)
    const content = page.locator('[class*="grid"], [class*="space-y"], .rounded-xl').first()
    await expect(content).toBeVisible({ timeout: 5000 })
  })

  test('themes page should load with grid layout', async ({ page }) => {
    await page.goto('/themes')
    await page.waitForLoadState('networkidle')
    
    // Check page loads without 404
    await expect(page).not.toHaveURL(/404/)
    
    // Check page heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Themes/i)
    
    // Page should render (even if no themes)
    await expect(page.locator('body')).toBeVisible()
    
    // Check for grid layout or empty state
    const content = page.locator('[class*="grid"], .rounded-2xl, .text-center').first()
    await expect(content).toBeVisible({ timeout: 5000 })
  })

  test('search page should load with enhanced search form', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')
    
    // Check page loads without 404
    await expect(page).not.toHaveURL(/404/)
    
    // Check page heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Search/i)
    
    // Check for large search input
    const searchInput = page.locator('input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    await expect(searchInput).toHaveAttribute('placeholder', /.+/)
    
    // Check for search button
    const searchButton = page.locator('button[type="submit"]').or(
      page.locator('button').filter({ hasText: /Search/i })
    )
    await expect(searchButton.first()).toBeVisible()
    
    // Check for search tips section
    const tipsSection = page.locator('text=/Search Tips|Popular Searches/i')
    await expect(tipsSection.first()).toBeVisible({ timeout: 3000 })
  })

  test('admin page should load', async ({ page }) => {
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')
    
    // Check page loads without 404
    await expect(page).not.toHaveURL(/404/)
    
    // Check page heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Admin/i)
  })

  test('admin monitoring page should load', async ({ page }) => {
    await page.goto('/admin/monitoring')
    await page.waitForLoadState('networkidle')
    
    // Check page loads without 404
    await expect(page).not.toHaveURL(/404/)
    
    // Check page heading
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
    await expect(heading).toContainText(/Monitoring/i)
  })

  test('pages should have consistent layout structure', async ({ page }) => {
    const pages = ['/', '/topics', '/themes', '/search']
    
    for (const path of pages) {
      await page.goto(path)
      await page.waitForLoadState('networkidle')
      
      // Check for main content area
      const main = page.locator('main').first()
      await expect(main).toBeVisible()
      
      // Check for page heading
      const heading = page.locator('h1').first()
      await expect(heading).toBeVisible()
      
      // Check for navigation (sidebar or top bar)
      const nav = page.locator('nav, [role="navigation"]').first()
      await expect(nav).toBeVisible()
    }
  })

  test('pages should be responsive on mobile', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    
    const pages = ['/', '/topics', '/themes', '/search']
    
    for (const path of pages) {
      await page.goto(path)
      await page.waitForLoadState('networkidle')
      
      // Check that content is visible and not cut off
      const main = page.locator('main').first()
      await expect(main).toBeVisible()
      
      // Check that text is readable
      const bodyText = await page.locator('body').textContent()
      expect(bodyText?.length).toBeGreaterThan(0)
    }
  })
})
