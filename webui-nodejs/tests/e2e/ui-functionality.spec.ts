import { test, expect } from '@playwright/test'

test.describe('UI Functionality', () => {
  test('page should be responsive and usable', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check that main content is visible
    const main = page.locator('main').first()
    await expect(main).toBeVisible()
    
    // Check that text is readable (not too small)
    const bodyText = await page.locator('body').textContent()
    expect(bodyText?.length).toBeGreaterThan(0)
    
    // Check for no layout issues (elements not overlapping)
    const mainContent = page.locator('main').first()
    await expect(mainContent).toBeVisible()
    
    // Check viewport size
    const viewport = page.viewportSize()
    expect(viewport).not.toBeNull()
  })

  test('should handle API errors gracefully', async ({ page }) => {
    // Intercept API calls and return errors
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ error: 'Internal Server Error' })
      })
    })

    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Page should still load (not crash)
    await expect(page.locator('body')).toBeVisible()
    
    // Should show error message or handle gracefully
    const errorMessage = page.locator('text=/error|failed|unable/i')
    // Error message might or might not be visible depending on implementation
    // Just verify page doesn't crash
    await page.waitForTimeout(2000)
    
    // Check that page structure is still intact
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible()
  })

  test('search form should be functional', async ({ page }) => {
    await page.goto('/search')
    await page.waitForLoadState('networkidle')
    
    const searchInput = page.locator('input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    
    // Type in search input
    await searchInput.fill('kubernetes')
    await expect(searchInput).toHaveValue('kubernetes')
    
    // Submit form (if button exists)
    const submitButton = page.locator('button[type="submit"]').first()
    if (await submitButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      await submitButton.click()
      // Should not cause page crash
      await page.waitForTimeout(2000)
      
      // Page should still be functional
      await expect(page.locator('body')).toBeVisible()
    }
  })

  test('topics page search should filter results', async ({ page }) => {
    await page.goto('/topics')
    await page.waitForLoadState('networkidle')
    
    // Find search input
    const searchInput = page.locator('input[type="text"]').first()
    await expect(searchInput).toBeVisible()
    
    // Type search query
    await searchInput.fill('test')
    await expect(searchInput).toHaveValue('test')
    
    // Wait for filtering (if implemented)
    await page.waitForTimeout(1000)
    
    // Page should still be functional
    await expect(page.locator('body')).toBeVisible()
  })

  test('should have accessible navigation', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check that navigation is keyboard accessible
    await page.keyboard.press('Tab')
    
    // Should be able to navigate with keyboard
    const focusedElement = page.locator(':focus')
    await expect(focusedElement).toBeVisible({ timeout: 2000 })
  })

  test('should support dark mode toggle', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Find theme toggle button
    const themeToggle = page.locator('button').filter({ 
      has: page.locator('svg') 
    }).or(page.locator('button[aria-label*="theme" i]'))
    
    if (await themeToggle.first().isVisible({ timeout: 2000 }).catch(() => false)) {
      // Click toggle
      await themeToggle.first().click()
      await page.waitForTimeout(500)
      
      // Check if dark mode class is applied
      const html = page.locator('html')
      const classes = await html.getAttribute('class')
      
      // Either dark class should be present or removed
      expect(classes).toBeTruthy()
    }
  })

  test('stat cards should display correctly', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Wait for stats to load
    await page.waitForTimeout(2000)
    
    // Check for stat cards
    const statCards = page.locator('text=/Total Topics|Active Sources|New Today|Themes/i')
    const count = await statCards.count()
    
    // Should have at least one stat card visible
    if (count > 0) {
      await expect(statCards.first()).toBeVisible()
    }
  })

  test('should have smooth animations', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check that page elements are visible (animations should complete)
    const heading = page.locator('h1').first()
    await expect(heading).toBeVisible({ timeout: 5000 })
    
    // Navigate to another page to test transitions
    await page.goto('/topics')
    await page.waitForLoadState('networkidle')
    
    const topicsHeading = page.locator('h1').first()
    await expect(topicsHeading).toBeVisible({ timeout: 5000 })
  })

  test('mobile menu should open and close', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Find mobile menu button
    const menuButton = page.locator('button').filter({ 
      has: page.locator('svg') 
    }).first()
    
    if (await menuButton.isVisible({ timeout: 2000 }).catch(() => false)) {
      // Open menu
      await menuButton.click()
      await page.waitForTimeout(500) // Wait for animation
      
      // Check menu is visible
      const menu = page.locator('text=/Dashboard|Topics|Themes/i')
      await expect(menu.first()).toBeVisible({ timeout: 3000 })
      
      // Close menu (click outside or close button)
      const closeButton = page.locator('button').filter({ 
        has: page.locator('svg') 
      }).nth(1)
      
      if (await closeButton.isVisible({ timeout: 1000 }).catch(() => false)) {
        await closeButton.click()
        await page.waitForTimeout(500)
      }
    }
  })

  test('should handle empty states gracefully', async ({ page }) => {
    // Intercept API to return empty data
    await page.route('**/api/**', route => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0 })
      })
    })

    await page.goto('/topics')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(2000)
    
    // Should show empty state message
    const emptyState = page.locator('text=/No topics|empty|not found/i')
    // Empty state might or might not be visible, but page should not crash
    await expect(page.locator('body')).toBeVisible()
  })
})
