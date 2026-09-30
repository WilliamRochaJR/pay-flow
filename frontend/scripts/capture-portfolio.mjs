import { mkdir } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

import { chromium } from '@playwright/test'

const currentDirectory = dirname(fileURLToPath(import.meta.url))
const outputDirectory = resolve(currentDirectory, '../../docs/assets/portfolio')
const baseUrl = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:5173'

await mkdir(outputDirectory, { recursive: true })

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })

try {
  await page.goto(baseUrl)
  await page.getByRole('heading', { name: 'Acesse sua conta' }).waitFor()
  await page.screenshot({ path: resolve(outputDirectory, 'login.png'), fullPage: true })

  const email = `portfolio-${Date.now()}@example.com`
  await page.getByRole('link', { name: 'Criar conta' }).click()
  await page.getByLabel('Nome').fill('PayFlow Demo')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill('safe-password')
  await page.getByRole('button', { name: /^criar conta$/i }).click()
  await page.getByRole('heading', { name: 'Acesse sua conta' }).waitFor()
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill('safe-password')
  await page.getByRole('button', { name: /^entrar$/i }).click()

  await page.getByRole('heading', { name: 'Seu dinheiro, em movimento.' }).waitFor()
  await page.getByLabel('Valor').fill('125.50')

  const transferResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/transfers') &&
      response.request().method() === 'POST' &&
      response.status() === 201,
  )

  await page.getByRole('button', { name: /transferir agora/i }).click()
  await transferResponse
  await page.getByRole('status').waitFor()
  await page.screenshot({ path: resolve(outputDirectory, 'dashboard.png'), fullPage: true })
} finally {
  await browser.close()
}
