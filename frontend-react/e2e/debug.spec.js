import { test, expect } from '@playwright/test'
import { writeFileSync } from 'fs'

const ADMIN_USER = process.env.E2E_ADMIN_USER || 'admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASS || 'admin123'

test('debug login flow', async ({ page }) => {
  const errors = []
  page.on('pageerror', err => errors.push(err.message))

  await page.goto('/login-react')
  await expect(page.locator('#username')).toBeVisible()
  await page.fill('#username', ADMIN_USER)
  await page.fill('#password', ADMIN_PASS)
  await page.click('button[type="submit"]')
  await page.waitForTimeout(2000)

  const html = await page.content()
  writeFileSync('e2e/screenshots/page.html', html)
  writeFileSync('e2e/screenshots/info.json', JSON.stringify({
    url: page.url(),
    title: await page.title(),
    errors,
  }, null, 2))

  expect(errors).toHaveLength(0)
})
