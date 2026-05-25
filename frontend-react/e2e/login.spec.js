import { test, expect } from '@playwright/test'

const ADMIN_USER = process.env.E2E_ADMIN_USER || 'admin'
const ADMIN_PASS = process.env.E2E_ADMIN_PASS || 'admin123'

test.describe('Login flujo completo', () => {

  test('carga formulario login-react con campos', async ({ page }) => {
    await page.goto('/login-react')
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('button', { name: /iniciar sesión/i })).toBeVisible()
  })

  test('login admin exitoso redirige al dashboard', async ({ page }) => {
    await page.goto('/login-react')
    await page.fill('#username', ADMIN_USER)
    await page.fill('#password', ADMIN_PASS)
    await page.click('button[type="submit"]')
    await expect(page.locator('.kpi-card').first()).toBeVisible({ timeout: 15000 })
  })

  test('login fallido muestra mensaje de error', async ({ page }) => {
    await page.context().clearCookies()
    await page.goto('/login-react')
    await page.fill('#username', 'baduser')
    await page.fill('#password', 'badpass')
    await page.click('button[type="submit"]')
    await expect(page.locator('.form-error')).toBeVisible({ timeout: 10000 })
  })
})
