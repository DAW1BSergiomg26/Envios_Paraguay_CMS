import { test, expect } from '@playwright/test'

const ADMIN_USER = process.env.E2E_ADMIN_USER || 'admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASS || 'admin123'

test.describe('Dashboard admin', () => {

  test('dashboard carga después de login con stats', async ({ page }) => {
    await page.goto('/login-react')
    await page.fill('#username', ADMIN_USER)
    await page.fill('#password', ADMIN_PASS)
    await page.click('button[type="submit"]')
    await expect(page.locator('.stats-card').first()).toBeVisible({ timeout: 15000 })
  })

  test('dashboard muestra navbar con nombre de usuario', async ({ page }) => {
    await page.goto('/login-react')
    await page.fill('#username', ADMIN_USER)
    await page.fill('#password', ADMIN_PASS)
    await page.click('button[type="submit"]')
    await expect(page.locator('.navbar-user')).toBeVisible({ timeout: 15000 })
    await expect(page.locator('.navbar-user')).toContainText('admin')
  })
})
