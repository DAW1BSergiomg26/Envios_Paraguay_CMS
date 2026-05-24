import { test, expect } from '@playwright/test'

test('carga página de búsqueda tracking', async ({ page }) => {
  await page.goto('/tracking')
  await expect(page.locator('h2')).toContainText(/seguimiento/i)
  await expect(page.locator('input[name="codigo"]')).toBeVisible()
  await expect(page.getByRole('button', { name: /buscar/i })).toBeVisible()
})

test('búsqueda con código inexistente muestra error', async ({ page }) => {
  await page.goto('/tracking')
  await page.fill('input[name="codigo"]', 'NO-EXISTE-999')
  await page.click('button[type="submit"]')
  await expect(page.locator('.tracking-not-found-premium')).toBeVisible({ timeout: 10000 })
})
