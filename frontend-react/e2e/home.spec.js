import { test, expect } from '@playwright/test'

test('carga la página principal sin errores', async ({ page }) => {
  const errors = []
  page.on('pageerror', err => errors.push(err.message))

  await page.goto('/')
  await expect(page).toHaveTitle(/Monteastur/i)
  expect(errors).toHaveLength(0)
})

test('página tiene contenido visible', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('body')).toBeVisible()
  const content = await page.locator('body').innerText()
  expect(content.length).toBeGreaterThan(100)
})
