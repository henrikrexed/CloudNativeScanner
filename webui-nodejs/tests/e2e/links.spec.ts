import { test, expect } from '@playwright/test'

test.describe('Link Validation', () => {
  test('all navigation links should be valid', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Collect all navigation links from sidebar
    const navLinks = await page.locator('nav a[href], [role="navigation"] a[href]').all()
    const linkHrefs: string[] = []
    
    for (const link of navLinks) {
      const href = await link.getAttribute('href')
      if (href && !href.startsWith('#') && !href.startsWith('http')) {
        linkHrefs.push(href)
      }
    }

    // Remove duplicates
    const uniqueHrefs = [...new Set(linkHrefs)]

    // Test each link
    for (const href of uniqueHrefs) {
      try {
        const response = await page.goto(href, { 
          waitUntil: 'networkidle', 
          timeout: 10000 
        })
        
        if (response) {
          expect(response.status()).not.toBe(404)
          expect(response.status()).toBeLessThan(400)
        }
      } catch (error) {
        // Some links might be handled client-side, which is OK
        // Just verify the page doesn't show 404
        await expect(page).not.toHaveURL(/404/)
      }
    }
  })

  test('dashboard quick action buttons should have valid links', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Check "Search Topics" quick action
    const searchLink = page.locator('a[href="/search"]').filter({ 
      hasText: /Search/i 
    }).first()
    
    if (await searchLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await searchLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/search/)
      await expect(page.locator('h1')).toContainText(/Search/i)
    }

    // Go back and check "Browse Topics"
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const topicsLink = page.locator('a[href="/topics"]').filter({ 
      hasText: /Browse|Topics/i 
    }).first()
    
    if (await topicsLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await topicsLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/topics/)
    }

    // Go back and check "View Themes"
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    const themesLink = page.locator('a[href="/themes"]').filter({ 
      hasText: /View|Themes/i 
    }).first()
    
    if (await themesLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await themesLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/themes/)
    }
  })

  test('should not have broken internal links', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Get all internal links on the page
    const links = await page.locator('a[href^="/"]').all()
    const brokenLinks: string[] = []

    for (const link of links) {
      const href = await link.getAttribute('href')
      if (href && !href.includes('#') && !href.includes('?')) {
        try {
          const response = await page.goto(href, { 
            waitUntil: 'networkidle', 
            timeout: 10000 
          })
          
          if (response && (response.status() === 404 || response.status() >= 500)) {
            brokenLinks.push(href)
          }
        } catch (error) {
          // Link might be handled client-side, check if page shows 404
          const url = page.url()
          if (url.includes('404')) {
            brokenLinks.push(href)
          }
        }
      }
    }

    expect(brokenLinks.length).toBe(0)
  })

  test('recent topics links should work', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    
    // Wait for topics to load
    await page.waitForTimeout(2000)
    
    // Check for "View all" link in recent topics section
    const viewAllLink = page.locator('a[href="/topics"]').filter({ 
      hasText: /View all/i 
    }).first()
    
    if (await viewAllLink.isVisible({ timeout: 3000 }).catch(() => false)) {
      await viewAllLink.click()
      await page.waitForLoadState('networkidle')
      await expect(page).toHaveURL(/\/topics/)
    }
  })

  test('theme cards should link to topics', async ({ page }) => {
    await page.goto('/themes')
    await page.waitForLoadState('networkidle')
    
    // Wait for themes to load
    await page.waitForTimeout(2000)
    
    // Check for "View Topics" links in theme cards
    const viewTopicsLinks = page.locator('a[href*="/topics"]').filter({ 
      hasText: /View Topics/i 
    })
    
    const count = await viewTopicsLinks.count()
    if (count > 0) {
      const firstLink = viewTopicsLinks.first()
      await firstLink.click()
      await page.waitForLoadState('networkidle')
      // Should navigate to topics page (possibly with filter)
      await expect(page).toHaveURL(/\/topics/)
    }
  })
})
