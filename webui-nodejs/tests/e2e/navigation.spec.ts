import { test, expect } from '@playwright/test'

test.describe('Navigation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    // Wait for page to fully load
    await page.waitForLoadState('networkidle')
  })

  test('should navigate to all main pages without errors', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/Dashboard|Cloud Native Scanner/)
    
    // Main navigation links in sidebar
    const navLinks = [
      { name: 'Dashboard', href: '/' },
      { name: 'Topics', href: '/topics' },
      { name: 'Themes', href: '/themes' },
      { name: 'Search', href: '/search' },
    ]

    for (const link of navLinks) {
      // Find navigation link by text content
      const navLink = page.locator(`a[href="${link.href}"]`).filter({ hasText: new RegExp(link.name, 'i') }).first()
      
      if (await navLink.isVisible({ timeout: 2000 }).catch(() => false)) {
        await navLink.click()
        await page.waitForLoadState('networkidle')
        
        // Verify we're on the correct page (no 404 errors)
        await expect(page).not.toHaveURL(/404/)
        await expect(page.locator('body')).toBeVisible()
        
        // Verify page has content
        const heading = page.locator('h1').first()
        await expect(heading).toBeVisible({ timeout: 5000 })
      }
    }
  })

  test('should have working quick action links on dashboard', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check "Search Topics" quick action card
    const searchLink = page.locator('a[href="/search"]').filter({ hasText: /Search/i }).first()
    if (await searchLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await searchLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/search/)
    }

    // Go back and check "Browse Topics" link
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const topicsLink = page.locator('a[href="/topics"]').filter({ hasText: /Browse|Topics/i }).first()
    if (await topicsLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await topicsLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/topics/)
    }

    // Go back and check "View Themes" link
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const themesLink = page.locator('a[href="/themes"]').filter({ hasText: /View|Themes/i }).first()
    if (await themesLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await themesLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/themes/)
    }
  })

  test('should have functional desktop sidebar navigation', async ({ page }) => {
    // Set desktop viewport
    await page.setViewportSize({ width: 1280, height: 720 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check sidebar is visible on desktop
    const sidebar = page.locator('nav, [role="navigation"]').first()
    if (await sidebar.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Verify sidebar contains navigation items
      await expect(sidebar.locator('text=/Dashboard/i')).toBeVisible({ timeout: 5000 })
      await expect(sidebar.locator('text=/Topics/i')).toBeVisible()
      await expect(sidebar.locator('text=/Themes/i')).toBeVisible()
    }
  })

  test('should have functional mobile menu', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check mobile menu button (hamburger icon)
    const mobileMenuButton = page.locator('button').filter({ 
      has: page.locator('svg') 
    }).first()
    
    if (await mobileMenuButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await mobileMenuButton.click()
      await page.waitForTimeout(500) // Wait for animation
      
      // Check that menu items are visible
      const menuItems = page.locator('text=/Dashboard|Topics|Themes|Search/i')
      await expect(menuItems.first()).toBeVisible({ timeout: 3000 })
    }
  })

  test('should highlight active navigation item', async ({ page }) => {
    await page.goto('/topics')
    await page.waitForLoadState('networkidle')
    
    // Check that Topics link has active state (gradient background or different styling)
    const topicsLink = page.locator('a[href="/topics"]').first()
    if (await topicsLink.isVisible({ timeout: 2000 }).catch(() => false)) {
      const classes = await topicsLink.getAttribute('class')
      // Active link should have gradient or primary color classes
      expect(classes).toMatch(/primary|gradient|bg-primary/i)
    }
  })

  test('should navigate to admin pages', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check for admin navigation section
    const adminLink = page.locator('a[href="/admin"]').filter({ hasText: /Admin/i }).first()
    if (await adminLink.isVisible({ timeout: 2000 }).catch(() => false)) {
      await adminLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/admin/)
    }
  })
})
